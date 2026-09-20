#!/usr/bin/env python3
"""
Patch dataTable.js to support |api:: and |columns:: in the input DSL.
Safe to re-run — detects and skips already-applied changes.
"""

import sys
from pathlib import Path

path = Path("ClientSide/FrameWorksLib/Jali.js/custom_elements/dataTable.js")

if not path.exists():
    print(f"ERROR: {path} not found. Run from project root.")
    sys.exit(1)

content = path.read_text(encoding="utf-8")
original = content

# ============================================================
# 1. PARSER — support |api:: and |columns::
# ============================================================

OLD_PARSER = '''for(let i = 0; i<1000 && tempText.indexOf("|value::") != -1 ; i++)
        {
            let tempNameindx = tempText.indexOf("|name::");
            let tempTitleindx = tempText.indexOf("|title::");
            let tempTypeindx = tempText.indexOf("|type::");
            let tempValueindx = tempText.indexOf("|value::");
            let tempSemiColumn = tempText.indexOf(";;");

            let tempName = tempText.slice(tempNameindx + "|name::".length, tempTitleindx);
            let tempTitle = tempText.slice(tempTitleindx + "|title::".length, tempTypeindx);
            let tempType = tempText.slice(tempTypeindx + "|type::".length, tempValueindx);
            let tempDefaultValue = tempText.slice(tempValueindx + "|value::".length, tempSemiColumn);'''

NEW_PARSER = '''for (let i = 0; i < 1000; i++)
        {
            const blockEnd = tempText.indexOf(";;");
            if (blockEnd === -1) break;

            const block = tempText.substring(0, blockEnd);

            const extractField = (blk, marker) => {
                const markers = ["|name::", "|title::", "|type::", "|value::", "|api::", "|columns::"];
                const idx = blk.indexOf(marker);
                if (idx === -1) return "";
                const start = idx + marker.length;
                let end = blk.length;
                for (const m of markers) {
                    if (m === marker) continue;
                    const p = blk.indexOf(m, start);
                    if (p !== -1 && p < end) end = p;
                }
                return blk.substring(start, end).trim();
            };

            const tempName = extractField(block, "|name::");
            const tempTitle = extractField(block, "|title::");
            const tempType = extractField(block, "|type::");
            const tempDefaultValue = extractField(block, "|value::");
            const tempApi = extractField(block, "|api::");
            const tempColumns = extractField(block, "|columns::");'''

if OLD_PARSER in content:
    content = content.replace(OLD_PARSER, NEW_PARSER, 1)
    print("✓ [1/4] Parser block patched")
elif "const extractField = (blk, marker)" in content:
    print("· [1/4] Parser already patched — skipping")
else:
    print("! [1/4] Parser block NOT FOUND — manual patch needed")


# ============================================================
# 2. LOOP TRAILER — use blockEnd instead of tempSemiColumn
# ============================================================

OLD_TRAILER = '''            tempText = tempText.slice(tempSemiColumn + 2);
        }'''

NEW_TRAILER = '''            tempText = tempText.substring(blockEnd + 2);
        }'''

if OLD_TRAILER in content:
    content = content.replace(OLD_TRAILER, NEW_TRAILER, 1)
    print("✓ [2/4] Loop trailer patched")
elif "tempText.substring(blockEnd + 2)" in content:
    print("· [2/4] Loop trailer already patched — skipping")
else:
    print("! [2/4] Loop trailer NOT FOUND — manual patch needed")


# ============================================================
# 3. DATA-COMBO — add api="..."
# ============================================================

OLD_COMBO = '''                        <data-combo
                            class="formInputform${formId}"
                            type="data-combo"
                            id="${tempName}"
                            name="${tempName}"
                            title="${tempTitle}"
                            value="${tempDefaultValue || ""}">
                        </data-combo>'''

NEW_COMBO = '''                        <data-combo
                            class="formInputform${formId}"
                            type="data-combo"
                            id="${tempName}"
                            name="${tempName}"
                            title="${tempTitle}"
                            api="${tempApi}"
                            value="${tempDefaultValue || ""}">
                        </data-combo>'''

if 'api="${tempApi}"' in content and 'type="data-combo"' in content:
    print("· [3/4] Data-combo api already present — skipping")
elif OLD_COMBO in content:
    content = content.replace(OLD_COMBO, NEW_COMBO, 1)
    print("✓ [3/4] Data-combo api attribute added")
else:
    print("! [3/4] Data-combo template NOT FOUND — manual patch needed")


# ============================================================
# 4. FIND-OBJECT-BOX — add api + columns
# ============================================================

OLD_FOB = '''                        <find-object-box
                            class="formInputform${formId}"
                            type="find-object-box"
                            id="${tempName}"
                            name="${tempName}"
                            title="${tempTitle}"
                            value="${tempDefaultValue || ""}">
                        </find-object-box>'''

NEW_FOB = '''                        <find-object-box
                            class="formInputform${formId}"
                            type="find-object-box"
                            id="${tempName}"
                            name="${tempName}"
                            title="${tempTitle}"
                            api="${tempApi}"
                            columns="${tempColumns}"
                            value="${tempDefaultValue || ""}">
                        </find-object-box>'''

if 'columns="${tempColumns}"' in content and 'type="find-object-box"' in content:
    print("· [4/4] FOB api/columns already present — skipping")
elif OLD_FOB in content:
    content = content.replace(OLD_FOB, NEW_FOB, 1)
    print("✓ [4/4] FOB api + columns attributes added")
else:
    print("! [4/4] FOB template NOT FOUND — manual patch needed")


# ============================================================
# SAVE
# ============================================================

print()

if content != original:
    path.write_text(content, encoding="utf-8")
    print(f"✅ Saved: {path}")
else:
    print("⚠  No changes written.")
    print("   If any step said 'NOT FOUND', paste the exact")
    print("   section from your dataTable.js and I'll re-target.")
