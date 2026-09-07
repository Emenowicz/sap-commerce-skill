#!/bin/bash

set -eu

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

printf 'INSERT_UPDATE Product;code[unique=true];name\n;sku-1;Example\n' > "$TMP_DIR/valid.impex"
printf 'GARBAGE Product code name\nthis is not impex\n' > "$TMP_DIR/invalid.impex"

"$ROOT_DIR/sap-commerce-cloud/scripts/validate-impex.sh" "$TMP_DIR/valid.impex" >/dev/null

if "$ROOT_DIR/sap-commerce-cloud/scripts/validate-impex.sh" "$TMP_DIR/invalid.impex" >/dev/null 2>&1; then
    echo "Expected invalid ImpEx input to fail"
    exit 1
fi

if "$ROOT_DIR/sap-commerce-cloud/scripts/query-items.sh" "UPDATE Product SET code='x'" >/dev/null 2>&1; then
    echo "Expected non-SELECT FlexibleSearch input to fail"
    exit 1
fi

if ! grep -q '2211-jdk21' "$ROOT_DIR/sap-commerce-cloud/SKILL.md"; then
    echo "Expected skill baseline to target 2211-jdk21"
    exit 1
fi

if grep -R -n -E 'javax\.(annotation|validation)|io\.swagger\.annotations|de\.hybris\.platform\.store\.services\.WarehouseService|getStockLevelForProductAndWarehouse' \
        "$ROOT_DIR/sap-commerce-cloud/assets"; then
    echo "Found APIs incompatible with the 2211-jdk21 baseline"
    exit 1
fi

if grep -R -n -E '"commerceSuiteVersion"[[:space:]]*:[[:space:]]*"2211"|@spartacus/[^[:space:]]*@latest|commercewebservices\.allowedOrigins|commercewebservices\.allowedCredentials' \
        "$ROOT_DIR/README.md" "$ROOT_DIR/sap-commerce-cloud"; then
    echo "Found stale 2211/JDK 17 configuration"
    exit 1
fi

if grep -R -n -E 'authorizedGrantTypes[^[:cntrl:]]*(^|,)password(,|;)|sap\.oauth2\.anonymous\.token\.enabled=true' \
        "$ROOT_DIR/sap-commerce-cloud"; then
    echo "Found OAuth configuration removed from 2211-jdk21"
    exit 1
fi

if grep -R -n -E '"oauth2"|de\.hybris\.platform\.oauth2\.' \
        "$ROOT_DIR/sap-commerce-cloud"; then
    echo "Found references to the removed oauth2 extension"
    exit 1
fi

if grep -R -n -E 'StockLevel AS [A-Za-z]+ ON \{p\.pk\}[[:space:]]*=[[:space:]]*\{[A-Za-z]+\.product\}' \
        "$ROOT_DIR/sap-commerce-cloud"; then
    echo "Found obsolete Product-to-StockLevel relation join"
    exit 1
fi

if grep -R -n '<mvc:annotation-driven' "$ROOT_DIR/sap-commerce-cloud/assets/occ-customization"; then
    echo "OCC customization must use the MVC infrastructure supplied by Commerce"
    exit 1
fi

for method in POST PUT DELETE; do
    if ! grep -A2 "RequestMethod\.$method" \
            "$ROOT_DIR/sap-commerce-cloud/assets/occ-customization/CustomProductController.java" \
            | grep -q '@Secured'; then
        echo "Expected $method OCC endpoint to declare an authorization role"
        exit 1
    fi
done

python3 - "$ROOT_DIR" <<'PY'
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
assets = root / "sap-commerce-cloud" / "assets"
declared = set()
errors = []

for path in assets.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if "@RequestMapping" in text and not re.search(r"@(?:Rest)?Controller\b", text):
        errors.append(f"{path}: MVC mappings require a controller stereotype")
    package = re.search(r"^package\s+([\w.]+);", text, re.MULTILINE)
    public_type = re.search(
        r"^public\s+(?:abstract\s+|final\s+)?(?:class|interface|enum|record)\s+(\w+)",
        text,
        re.MULTILINE,
    )
    if not package or not public_type:
        continue
    name = public_type.group(1)
    declared.add(f"{package.group(1)}.{name}")
    if path.stem != name:
        errors.append(f"{path}: public type {name} must match the file name")

for path in assets.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    for imported in re.findall(r"^import\s+(com\.example\.[\w.]+);", text, re.MULTILINE):
        if imported not in declared:
            errors.append(f"{path}: imported example type is missing: {imported}")

bean_names = {}
for path in assets.rglob("*spring.xml"):
    for element in ET.parse(path).getroot().iter():
        name = element.attrib.get("id") or element.attrib.get("alias")
        if name:
            if name in bean_names:
                errors.append(f"{path}: duplicate bean name {name}, also in {bean_names[name]}")
            bean_names[name] = path
        class_name = element.attrib.get("class", "")
        if class_name.startswith("com.example.") and class_name not in declared:
            errors.append(f"{path}: active Spring class is missing: {class_name}")

if errors:
    raise SystemExit("\n".join(errors))
PY

echo "Skill checks passed"
