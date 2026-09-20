#!/bin/bash
# ============================================================
# IFMS — Frontend restructure
# ============================================================
# Safe to re-run. Overwrites the files it manages.
# Run from the project root (the folder that contains ClientSide/).
# ============================================================

set -e

CLIENT="ClientSide"

if [ ! -d "$CLIENT" ]; then
    echo "ERROR: ClientSide/ not found. Run this from the project root."
    exit 1
fi

echo "→ Creating AccessManagement directory"
mkdir -p "$CLIENT/AccessManagement"


# ============================================================
# ACCESS MANAGEMENT — LANDING PAGE
# ============================================================

cat > "$CLIENT/AccessManagement/AccessManagement.html" <<'HTML'
<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>Access Management</title>

    <script type="module" src="http://127.0.0.1:8080/coreJs"></script>

    <style>
        :root {
            --background: #f8fafc;
            --card-background: #ffffff;
            --text: #1e293b;
            --secondary-text: #64748b;
            --border: #e2e8f0;
            --hover: #f1f5f9;
            --shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
            --shadow-hover: 0 8px 18px rgba(0, 0, 0, 0.12);
            --accent: #3b82f6;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            min-height: 100vh;
            padding: 30px;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--background);
            color: var(--text);
        }

        .page-header { margin-bottom: 30px; }
        .page-header h1 { margin-bottom: 8px; font-size: 2rem; font-weight: 600; }
        .page-header p  { color: var(--secondary-text); font-size: 1rem; }

        .forms-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 18px;
        }

        .form-card {
            width: 100%;
            min-height: 150px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: flex-start;
            padding: 22px;
            border: 1px solid var(--border);
            border-radius: 12px;
            background-color: var(--card-background);
            color: var(--text);
            box-shadow: var(--shadow);
            cursor: pointer;
            text-align: left;
            transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
        }

        .form-card:hover {
            transform: translateY(-2px);
            background-color: var(--hover);
            box-shadow: var(--shadow-hover);
        }

        .form-card:active { transform: translateY(0); }

        .form-icon        { margin-bottom: 14px; font-size: 2.1rem; }
        .form-title       { font-size: 1.05rem; font-weight: 600; }
        .form-description { margin-top: 6px; color: var(--secondary-text); font-size: 0.88rem; line-height: 1.4; }
    </style>

</head>

<body>

    <header class="page-header">
        <h1>Access Management</h1>
        <p>Submit and manage access requests and stock-adjustment tickets.</p>
    </header>

    <main class="forms-grid">

        <button
            type="button"
            class="form-card"
            data-AccessCode="300"
            onclick="window.parent.openDashboardTab('Access Requests', '/accessRequests');"
        >
            <div class="form-icon">🎫</div>
            <div class="form-title">Access Requests</div>
            <div class="form-description">
                Submit and review temp-access and stock-adjustment tickets.
            </div>
        </button>

    </main>

</body>

</html>
HTML

echo "  ✓ AccessManagement/AccessManagement.html"


# ============================================================
# ACCESS REQUEST PLACEHOLDER PAGES
# ============================================================

write_placeholder () {
    local path="$1"
    local title="$2"
    local subtitle="$3"

    cat > "$path" <<HTML
<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>${title}</title>

    <script type="module" src="http://127.0.0.1:8080/coreJs"></script>

    <style>
        body {
            margin: 0;
            min-height: 100vh;
            padding: 25px;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f8fafc;
            color: #1e293b;
        }

        .page-header { margin-bottom: 25px; }
        .page-header h1 { margin: 0 0 8px 0; font-size: 2rem; font-weight: 600; }
        .page-header p  { margin: 0; color: #64748b; font-size: 0.95rem; }

        .placeholder {
            margin-top: 30px;
            padding: 50px 20px;
            border: 2px dashed #cbd5e1;
            border-radius: 12px;
            background-color: #ffffff;
            text-align: center;
            color: #94a3b8;
            font-size: 0.95rem;
        }
    </style>

</head>

<body>

    <div class="page-header">
        <h1>${title}</h1>
        <p>${subtitle}</p>
    </div>

    <div class="placeholder">
        Under construction — this form will be built in a later phase.
    </div>

</body>

</html>
HTML

    echo "  ✓ ${path#ClientSide/}"
}

write_placeholder "$CLIENT/AccessManagement/AccessRequests.html" \
    "Access Requests" \
    "Submit and review temp-access and stock-adjustment tickets."

write_placeholder "$CLIENT/AccessManagement/AccessRequestObject.html" \
    "Access Request — Object Details" \
    "Per-object permission payload for temp-access tickets."

write_placeholder "$CLIENT/AccessManagement/AccessRequestStock.html" \
    "Access Request — Stock Details" \
    "Per-product stock-adjustment payload."


# ============================================================
# FLEET MANAGEMENT — LANDING PAGE (overwrite)
# ============================================================

cat > "$CLIENT/FleetManagement/FleetManagement.html" <<'HTML'
<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>Fleet Management</title>

    <script type="module" src="http://127.0.0.1:8080/coreJs"></script>

    <style>
        :root {
            --background: #f8fafc;
            --card-background: #ffffff;
            --text: #1e293b;
            --secondary-text: #64748b;
            --border: #e2e8f0;
            --hover: #f1f5f9;
            --shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
            --shadow-hover: 0 8px 18px rgba(0, 0, 0, 0.12);
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            min-height: 100vh;
            padding: 30px;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--background);
            color: var(--text);
        }

        .page-header { margin-bottom: 30px; }
        .page-header h1 { margin-bottom: 8px; font-size: 2rem; font-weight: 600; }
        .page-header p  { color: var(--secondary-text); font-size: 1rem; }

        .forms-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 18px;
        }

        .form-card {
            width: 100%;
            min-height: 150px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: flex-start;
            padding: 22px;
            border: 1px solid var(--border);
            border-radius: 12px;
            background-color: var(--card-background);
            color: var(--text);
            box-shadow: var(--shadow);
            cursor: pointer;
            text-align: left;
            transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
        }

        .form-card:hover {
            transform: translateY(-2px);
            background-color: var(--hover);
            box-shadow: var(--shadow-hover);
        }

        .form-card:active { transform: translateY(0); }

        .form-icon        { margin-bottom: 14px; font-size: 2.1rem; }
        .form-title       { font-size: 1.05rem; font-weight: 600; }
        .form-description { margin-top: 6px; color: var(--secondary-text); font-size: 0.88rem; line-height: 1.4; }
    </style>

</head>

<body>

    <header class="page-header">
        <h1>Fleet Management</h1>
        <p>Select a fleet form to continue.</p>
    </header>

    <main class="forms-grid">

        <button type="button" class="form-card" data-AccessCode="200"
            onclick="window.parent.openDashboardTab('Fleet Team', '/fleetTeam');">
            <div class="form-icon">🚚</div>
            <div class="form-title">Fleet Teams</div>
            <div class="form-description">Define and manage fleet teams.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="201"
            onclick="window.parent.openDashboardTab('Drivers', '/drivers');">
            <div class="form-icon">🧑‍✈️</div>
            <div class="form-title">Drivers</div>
            <div class="form-description">Manage driver records.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="202"
            onclick="window.parent.openDashboardTab('Vehicles', '/vehicles');">
            <div class="form-icon">🚛</div>
            <div class="form-title">Vehicles</div>
            <div class="form-description">Manage vehicle records.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="203"
            onclick="window.parent.openDashboardTab('Team Drivers', '/fleetTeamDrivers');">
            <div class="form-icon">👥</div>
            <div class="form-title">Team Drivers</div>
            <div class="form-description">Assign drivers and vehicles to teams.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="204"
            onclick="window.parent.openDashboardTab('Team Managers', '/fleetTeamManager');">
            <div class="form-icon">🧑‍💼</div>
            <div class="form-title">Team Managers</div>
            <div class="form-description">Assign users as fleet team managers.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="205"
            onclick="window.parent.openDashboardTab('Fleet Trips', '/fleetTrip');">
            <div class="form-icon">🗺️</div>
            <div class="form-title">Fleet Trips</div>
            <div class="form-description">Plan routes, distance and duration.</div>
        </button>

        <button type="button" class="form-card" data-AccessCode="207"
            onclick="window.parent.openDashboardTab('Team Transports', '/fleetTeamTransports');">
            <div class="form-icon">📦</div>
            <div class="form-title">Team Transports</div>
            <div class="form-description">Assign teams to product-request transports.</div>
        </button>

    </main>

</body>

</html>
HTML

echo "  ✓ FleetManagement/FleetManagement.html"


# ============================================================
# FLEET FORM PLACEHOLDERS
# ============================================================

write_placeholder "$CLIENT/FleetManagement/FleetTeam.html" \
    "Fleet Teams" \
    "Define and manage fleet teams."

write_placeholder "$CLIENT/FleetManagement/Drivers.html" \
    "Drivers" \
    "Manage driver records."

write_placeholder "$CLIENT/FleetManagement/Vehicles.html" \
    "Vehicles" \
    "Manage vehicle records."

write_placeholder "$CLIENT/FleetManagement/FleetTeamDrivers.html" \
    "Team Drivers" \
    "Assign drivers and vehicles to teams."

write_placeholder "$CLIENT/FleetManagement/FleetTeamManager.html" \
    "Team Managers" \
    "Assign users as fleet team managers."

write_placeholder "$CLIENT/FleetManagement/FleetTrip.html" \
    "Fleet Trips" \
    "Plan routes, distance and duration."

write_placeholder "$CLIENT/FleetManagement/FleetTeamTransports.html" \
    "Team Transports" \
    "Assign teams to product-request transports."


# ============================================================
# PATCH Dashboard.html — add Access Management sidebar item
# ============================================================

echo "→ Patching Dashboard.html"

python3 - <<'PY'
import re, sys

path = "ClientSide/Dashboard/Dashboard.html"

with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# --- 1. Insert Access Management sidebar item after Reports ---

reports_block = """            Reports

        </div>

    </div>"""

access_block = """            Reports

        </div>


        <!-- OBJECT 50 -->

        <div
            class="menu-item"
            data-AccessCode="50"
            data-route="/accessManagement"
        >

            <i>🎫</i>

            Access Management

        </div>

    </div>"""

if "data-AccessCode=\"50\"" in content:
    print("  · Access Management sidebar item already present — skipping")
elif reports_block in content:
    content = content.replace(reports_block, access_block, 1)
    print("  ✓ Added Access Management sidebar item")
else:
    print("  ! Could not find Reports block. Add the sidebar item manually.")
    print("    Insert this before the sidebar closing </div>:")
    print("""
        <!-- OBJECT 50 -->

        <div
            class="menu-item"
            data-AccessCode="50"
            data-route="/accessManagement"
        >

            <i>🎫</i>

            Access Management

        </div>
""")

# --- 2. Add route mapping ---

routes_old = """                '/reports':
                    'Reports'"""

routes_new = """                '/reports':
                    'Reports',

                '/accessManagement':
                    'Access Management'"""

if "'/accessManagement'" in content:
    print("  · Route mapping already present — skipping")
elif routes_old in content:
    content = content.replace(routes_old, routes_new, 1)
    print("  ✓ Added /accessManagement route mapping")
else:
    print("  ! Could not find routes object. Add this manually inside the routes object:")
    print("                '/accessManagement':\n                    'Access Management'")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
PY


# ============================================================
# SUMMARY
# ============================================================

echo ""
echo "============================================================"
echo " Frontend restructure complete."
echo "============================================================"
echo ""
echo " New files created:"
find "$CLIENT/AccessManagement" -type f | sort | sed 's|^|   |'
echo ""
echo " Updated files:"
echo "   ClientSide/Dashboard/Dashboard.html"
echo "   ClientSide/FleetManagement/FleetManagement.html"
echo ""
echo " Placeholders awaiting content (Phase 3):"
echo "   ClientSide/AccessManagement/AccessRequests.html"
echo "   ClientSide/AccessManagement/AccessRequestObject.html"
echo "   ClientSide/AccessManagement/AccessRequestStock.html"
echo "   ClientSide/FleetManagement/FleetTeam.html"
echo "   ClientSide/FleetManagement/Drivers.html"
echo "   ClientSide/FleetManagement/Vehicles.html"
echo "   ClientSide/FleetManagement/FleetTeamDrivers.html"
echo "   ClientSide/FleetManagement/FleetTeamManager.html"
echo "   ClientSide/FleetManagement/FleetTrip.html"
echo "   ClientSide/FleetManagement/FleetTeamTransports.html"
echo ""
