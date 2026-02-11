#!/bin/bash
# query-items.sh
# Execute FlexibleSearch queries via HAC Scripting Console
# Usage: ./query-items.sh "<query>" [--csv]
#
# Environment variables:
#   HAC_URL      - HAC URL (default: https://localhost:9002/hac)
#   HAC_USER     - HAC username (default: admin)
#   HAC_PASSWORD - HAC password (required)

set -e

# Default configuration
HAC_URL="${HAC_URL:-https://localhost:9002/hac}"
HAC_USER="${HAC_USER:-admin}"
OUTPUT_CSV=false

# Parse arguments
QUERY=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --csv)
            OUTPUT_CSV=true
            shift
            ;;
        --url)
            HAC_URL="$2"
            shift 2
            ;;
        --user)
            HAC_USER="$2"
            shift 2
            ;;
        --password)
            HAC_PASSWORD="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 \"<query>\" [options]"
            echo ""
            echo "Options:"
            echo "  --csv          Output results in CSV format"
            echo "  --url URL      HAC URL (default: $HAC_URL)"
            echo "  --user USER    HAC username (default: admin)"
            echo "  --password PWD HAC password"
            echo ""
            echo "Environment variables:"
            echo "  HAC_URL, HAC_USER, HAC_PASSWORD"
            echo ""
            echo "Example:"
            echo "  $0 \"SELECT {pk}, {code} FROM {Product} WHERE {code} LIKE '%test%'\""
            echo "  $0 \"SELECT COUNT({pk}) FROM {Order}\" --csv"
            exit 0
            ;;
        *)
            if [ -z "$QUERY" ]; then
                QUERY="$1"
            fi
            shift
            ;;
    esac
done

# Check required parameters
if [ -z "$QUERY" ]; then
    echo "Error: Query is required"
    echo "Usage: $0 \"<query>\" [--csv]"
    exit 1
fi

if [ -z "$HAC_PASSWORD" ]; then
    echo "Error: Password is required"
    echo "Set HAC_PASSWORD environment variable or use --password option"
    exit 1
fi

COOKIE_FILE=$(mktemp /tmp/hac_cookies.XXXXXX)
trap "rm -f $COOKIE_FILE" EXIT

echo "=== FlexibleSearch Query Executor ==="
echo "HAC URL: $HAC_URL"
echo "Query: $QUERY"
echo ""

# Create Groovy script to execute query
SCRIPT=$(cat << 'GROOVY'
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import de.hybris.platform.servicelayer.search.FlexibleSearchService

def flexibleSearchService = spring.getBean("flexibleSearchService")

def query = new FlexibleSearchQuery(QUERY_PLACEHOLDER)
def result = flexibleSearchService.search(query)

println "Results: ${result.count} of ${result.totalCount}"
println "---"

result.result.each { item ->
    if (item instanceof de.hybris.platform.core.model.ItemModel) {
        println "PK: ${item.pk} | Type: ${item.itemtype}"
    } else if (item instanceof Object[]) {
        println item.collect { it?.toString() ?: 'null' }.join(' | ')
    } else {
        println item?.toString() ?: 'null'
    }
}
GROOVY
)

# Replace placeholder with actual query (escape for Groovy)
ESCAPED_QUERY=$(echo "$QUERY" | sed 's/"/\\"/g')
SCRIPT="${SCRIPT//QUERY_PLACEHOLDER/\"$ESCAPED_QUERY\"}"

# Get CSRF token first
echo "Authenticating..."
CSRF_RESPONSE=$(curl -s -c $COOKIE_FILE -b $COOKIE_FILE \
    -u "$HAC_USER:$HAC_PASSWORD" \
    -k "$HAC_URL/console/scripting" 2>/dev/null)

CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p; s/.*value="\([^"]*\)"[^>]*name="_csrf".*/\1/p' | head -1)

if [ -z "$CSRF_TOKEN" ]; then
    echo "Error: Could not obtain CSRF token. Check credentials and HAC URL."
    exit 1
fi

echo "Executing query..."

# Execute script via HAC
RESPONSE=$(curl -s -X POST \
    -c $COOKIE_FILE -b $COOKIE_FILE \
    -u "$HAC_USER:$HAC_PASSWORD" \
    -k "$HAC_URL/console/scripting/execute" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "_csrf=$CSRF_TOKEN" \
    -d "scriptType=groovy" \
    -d "commit=false" \
    --data-urlencode "script=$SCRIPT" 2>/dev/null)

# Parse response
if echo "$RESPONSE" | grep -q "executionResult"; then
    RESULT=$(echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('executionResult', 'No result'))
except:
    print(sys.stdin.read())
" 2>/dev/null || echo "$RESPONSE")

    echo ""
    echo "=== Results ==="
    echo "$RESULT"

    # CSV output
    if [ "$OUTPUT_CSV" = true ]; then
        echo ""
        echo "=== CSV Output ==="
        echo "$RESULT" | grep -E "^\s*PK:|^[0-9]" | sed 's/ | /,/g'
    fi
else
    echo "Error executing query:"
    echo "$RESPONSE"
    exit 1
fi

echo ""
echo "Done."
