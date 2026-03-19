#!/bin/bash
# validate-impex.sh
# Validate ImpEx file syntax before import
# Usage: ./validate-impex.sh <impex-file>

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

if [ -z "$1" ]; then
    echo "Usage: $0 <impex-file>"
    echo "Example: $0 products.impex"
    exit 1
fi

IMPEX_FILE="$1"

if [ ! -f "$IMPEX_FILE" ]; then
    echo -e "${RED}Error: File not found: $IMPEX_FILE${NC}"
    exit 1
fi

echo "=== ImpEx Validator ==="
echo "Validating: $IMPEX_FILE"
echo ""

ERRORS=0
WARNINGS=0
LINE_NUM=0

# Read file line by line
while IFS= read -r line || [ -n "$line" ]; do
    ((LINE_NUM++))

    # Skip empty lines and comments
    if [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]]; then
        continue
    fi

    # Check for common syntax errors

    # 1. Check for valid operation keywords
    if [[ "$line" =~ ^(INSERT|UPDATE|INSERT_UPDATE|REMOVE)[[:space:]] ]]; then
        # Valid operation header
        OPERATION=$(echo "$line" | grep -oE "^(INSERT|UPDATE|INSERT_UPDATE|REMOVE)")

        # Check for type declaration
        if [[ ! "$line" =~ ^(INSERT|UPDATE|INSERT_UPDATE|REMOVE)[[:space:]]+[A-Za-z] ]]; then
            echo -e "${RED}Line $LINE_NUM: Missing type after $OPERATION${NC}"
            ((ERRORS++))
        fi

        # Check for attribute declarations
        if [[ ! "$line" =~ \; ]]; then
            echo -e "${RED}Line $LINE_NUM: Missing semicolon in header${NC}"
            ((ERRORS++))
        fi
    fi

    # 2. Check for unbalanced brackets
    OPEN_PAREN=$(echo "$line" | tr -cd '(' | wc -c)
    CLOSE_PAREN=$(echo "$line" | tr -cd ')' | wc -c)
    if [ "$OPEN_PAREN" -ne "$CLOSE_PAREN" ]; then
        echo -e "${YELLOW}Line $LINE_NUM: Unbalanced parentheses (${OPEN_PAREN} open, ${CLOSE_PAREN} close)${NC}"
        ((WARNINGS++))
    fi

    OPEN_BRACKET=$(echo "$line" | tr -cd '[' | wc -c)
    CLOSE_BRACKET=$(echo "$line" | tr -cd ']' | wc -c)
    if [ "$OPEN_BRACKET" -ne "$CLOSE_BRACKET" ]; then
        echo -e "${YELLOW}Line $LINE_NUM: Unbalanced brackets (${OPEN_BRACKET} open, ${CLOSE_BRACKET} close)${NC}"
        ((WARNINGS++))
    fi

    # 3. Check for data rows (start with ;)
    if [[ "$line" =~ ^[[:space:]]*\; ]]; then
        # Count semicolons in data row
        SEMICOLONS=$(echo "$line" | tr -cd ';' | wc -c)
        if [ "$SEMICOLONS" -lt 2 ]; then
            echo -e "${YELLOW}Line $LINE_NUM: Data row has only $SEMICOLONS values${NC}"
            ((WARNINGS++))
        fi
    fi

    # 4. Check for undefined macros ($ without definition)
    if [[ "$line" =~ \$[a-zA-Z_][a-zA-Z0-9_]* && ! "$line" =~ ^[[:space:]]*\$[a-zA-Z_][a-zA-Z0-9_]*= ]]; then
        MACROS=$(echo "$line" | grep -oE '\$[a-zA-Z_][a-zA-Z0-9_]*' | sort -u)
        # This is just a warning - macros might be defined elsewhere
        for macro in $MACROS; do
            # Skip system-provided macros
            if [[ "$macro" =~ ^\$(config-|lang-|START_|END_) ]]; then
                continue
            fi
            if ! grep -q "^[[:space:]]*${macro}=" "$IMPEX_FILE"; then
                echo -e "${YELLOW}Line $LINE_NUM: Macro $macro may not be defined in this file${NC}"
                ((WARNINGS++))
            fi
        done
    fi

    # 5. Check for common typos
    if [[ "$line" =~ INSERT_UPATE || "$line" =~ INSRET || "$line" =~ UPDAT[^E] ]]; then
        echo -e "${RED}Line $LINE_NUM: Possible typo in operation keyword${NC}"
        ((ERRORS++))
    fi

    # 6. Check for missing unique modifier in headers
    if [[ "$line" =~ ^(INSERT_UPDATE|UPDATE)[[:space:]] ]]; then
        if [[ ! "$line" =~ \[unique=true\] && ! "$line" =~ \[unique=TRUE\] ]]; then
            echo -e "${YELLOW}Line $LINE_NUM: No [unique=true] attribute found - update may not match correctly${NC}"
            ((WARNINGS++))
        fi
    fi

done < "$IMPEX_FILE"

echo ""
echo "=== Validation Summary ==="
echo -e "Errors:   ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}Validation FAILED - Please fix errors before importing${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}Validation passed with warnings${NC}"
    exit 0
else
    echo -e "${GREEN}Validation PASSED${NC}"
    exit 0
fi
