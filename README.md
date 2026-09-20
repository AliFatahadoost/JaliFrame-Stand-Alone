Jali Frame

    A from-scratch Java full-stack framework. Enums declare your app. Custom elements render it. No controllers, no ORM, no build step.

https://img.shields.io/badge/Java-17%2B-orange
https://img.shields.io/badge/SQL%20Server-2019%2B-blue
https://img.shields.io/badge/License-MIT-green

Jali Frame is a convention-driven full-stack framework for building database-backed web applications in Java. It replaces the traditional MVC + ORM + REST stack with three enums and a library of Web Components.

One enum entry declares your table, its CRUD API, its page route, its permissions, its form fields, and its client-side rendering. The framework wires everything else.
java

// One enum entry
Warehouse(
    new ReadQuery().setTableName("warehouse").setColumnNames(
        "warehouse_id", "warehouse_title", "warehouse_code",
        "warehouse_lat", "warehouse_long"
    ).getQuery(),
    new UpdateQuery().setTableName("warehouse").setColumnNames(
        "warehouse_title", "warehouse_code",
        "warehouse_lat", "warehouse_long"
    ).setWhereQuery("warehouse_id = ?").getQuery(),
    new CreateQuery().setTableName("warehouse").setColumnNames(
        "warehouse_title", "warehouse_code",
        "warehouse_lat", "warehouse_long"
    ).getQuery(),
    "UPDATE warehouse SET is_deleted = 1 WHERE warehouse_id = ?",
    101  // object code for RBAC
),

// One HTML tag
<fetch-data-table
    api="/warehouseApi"
    inputs="
        |name::warehouse_title|title::Title|type::text|value::;;
        |name::warehouse_lat,warehouse_long|title::Location|type::map-box|value::;;
    "
    columns="Title, Location">
</fetch-data-table>

That's a full CRUD page. Search, pagination, create/edit modals, soft delete, permission gating, an interactive map — all from those two blocks.
Table of Contents

    Why Jali Frame?

    Core Concepts

    Quick Start

    Building Your First Page

    The Input DSL

    Custom Elements

    RBAC

    Directory Structure

    Configuration

    Database Conventions

    Architecture

    Extending the Framework

    Known Limitations

    Roadmap

    Contributing

    License

Why Jali Frame?

Most Java web frameworks assume you want controllers, annotations, DTOs, mappers, and a client framework. If you're building an internal CRUD application, that stack costs you weeks before the first row is saved.

Jali Frame takes a different position:

Application structure is data, not code.

Three enums (FilesEnum, WebPagesEnum, CrudQueriesEnum) describe the entire surface of your app. Adding a page is not "write a controller, write a route, write a service, write a repository, write a DTO". Adding a page is:

    One entry in CrudQueriesEnum (SQL)

    One entry in FilesEnum (asset path)

    One entry in WebPagesEnum (route + object code)

    One HTML file with <fetch-data-table>

That's it. The framework handles:

    Route registration

    API endpoints (GET/POST/PUT/DELETE)

    Parameter binding from URL + JSON body

    Session auth

    Per-object permission checks

    Server-side HTML trimming by access code

    Client-side table rendering, search, pagination

    Create/edit modals with validation

    Soft-delete convention

It's opinionated. You don't get to bring your own ORM. You don't get to bring React. You get a small, cohesive stack that ships internal tools fast.
Core Concepts
1. Object Codes

Every page gets a numeric object code (e.g. 101 for Warehouse). It's used three times:

    In WebPagesEnum — registers the page route

    In CrudQueriesEnum — ties every SQL query to a permission

    In HTML — data-AccessCode="101" for server-side trimming

One number, one permission boundary. Nothing falls through the cracks.
2. Enums Declare Everything
Enum	Declares
FileTypesEnum	MIME types (html, css, js, png)
FilesEnum	Every static asset + HTML page
WebPagesEnum	Every route + object code + auth requirement
CrudQueriesEnum	Every SQL query + permission binding
3. Positional Payloads

Create/Update requests send {input0, input1, …} in enum column order. This is the framework's biggest gotcha — input order in HTML must match setColumnNames(...) order in the enum. Named payloads are on the roadmap.
4. Server-Side HTML Trimming

Before any HTML page is sent to the browser, pageHandlerOpener walks the DOM and removes any element with a data-AccessCode the user lacks READ permission for.

The user literally doesn't receive the Warehouse card in their sidebar if they can't read Warehouse. No client-side flicker. No hidden-element exploits.
Quick Start
Requirements

    Java 17+

    SQL Server 2019+

    A web browser (any modern one — no build tools required)

1. Clone
bash

git clone https://github.com/yourname/jali-frame.git
cd jali-frame

2. Configure

Create config.txt next to the JAR:
properties

BASE_FILE_ADDRESS=/absolute/path/to/ClientSide
server=localhost
port=1433
databaseName=YOUR_DB_NAME
username=sa
password=your_password
MAX_CONNECTION_POOL=5
portNumber=8080
serverIP=127.0.0.1
queueWaitLine=10
MAX_SESSION_TIME=86400

3. Boot
bash

java -cp target/jali-frame.jar yourpackage.mainServerLaunch

You'll be prompted:

    Y — launch the Swing config GUI

    n — console mode with commands like launch, set, testdb

    s — silent boot (jump straight to serving)

On first boot, the framework:

    Creates USERS_DATA_AND_PERMISSIONS and INIT_DATABASE schemas

    Creates SYS_USERS, SYS_OBJECTS, OBJECT_USER_PERMISSION

    Installs stored procedures for auth + permission checks

    Creates default user ADMIN with password 12

4. Log in
text

http://127.0.0.1:8080/

Building Your First Page

Let's add a Book page from scratch.
Step 1 — Create the DB table
sql

CREATE TABLE dbo.book (
    book_id     NUMERIC(18,0) IDENTITY(1,1) PRIMARY KEY,
    book_title  NVARCHAR(100) NOT NULL,
    book_author NVARCHAR(50)  NOT NULL,
    book_year   NUMERIC(18,0) NULL,
    is_deleted  BIT           NULL DEFAULT 0
);

Step 2 — Add the enum entry

CrudQueriesEnum.java:
java

Book(
    new ReadQuery()
        .setTableName("book")
        .setColumnNames("book_id", "book_title", "book_author", "book_year")
        .setColumnDataTypes("numeric", "nvarchar(100)", "nvarchar(50)", "numeric")
        .setWhereQuery("AND isnull(is_deleted, 0) = 0")
        .getQuery(),
    new UpdateQuery()
        .setTableName("book")
        .setColumnNames("book_title", "book_author", "book_year")
        .setWhereQuery("book_id = ?")
        .getQuery(),
    new CreateQuery()
        .setTableName("book")
        .setColumnNames("book_title", "book_author", "book_year")
        .getQuery(),
    "UPDATE book SET is_deleted = 1 WHERE book_id = ?",
    500  // new object code
),

FilesEnum.java:
java

Book("/Books/Book.html", false, FileTypesEnum.html),

WebPagesEnum.java:
java

Book(500, FilesEnum.Book, "/book"),

Step 3 — Register the route

mainServerLaunch.java:
java

server.createContext("/bookApi",
    new apiManagement.dataApiGen.builder()
        .shouldAuthenticate(true).sendTokenToDB(false)
        .setQuery(CrudQueriesEnum.Book)
        .build()
);

WebPagesEnum.Book.registerRoute(server);

// At the very end of main():
DataBaseInit.finalizeRegistration();

Step 4 — Create the HTML file

ClientSide/Books/Book.html:
html

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Books</title>
    <script type="module" src="http://127.0.0.1:8080/coreJs"></script>
</head>
<body style="padding:25px;font-family:system-ui">

    <h1>Books</h1>

    <fetch-data-table
        data-AccessCode="500"
        api="http://127.0.0.1:8080/bookApi"
        table-id="bookTable"
        caption="Library"
        classes="blue neon pastel"
        inputs="
            |name::book_title|title::Title|type::text|value::;;
            |name::book_author|title::Author|type::text|value::;;
            |name::book_year|title::Year|type::number|value::;;
        "
        columns="Title, Author, Year">
    </fetch-data-table>

</body>
</html>

Step 5 — Restart
bash

mvn clean compile
java -cp target/jali-frame.jar yourpackage.mainServerLaunch

Hard-refresh. Navigate to /book. You now have:

    A table with search + pagination

    A "+" button that opens a create modal

    An "✎" button for edit

    A "🗑" button for soft delete

    Full RBAC integration with object code 500

    The framework auto-created a permission row for every user

You wrote ~40 lines of Java and 30 lines of HTML. That's the whole loop.
The Input DSL

Every form field is a ;;-terminated block inside the inputs attribute:
text

|name::FIELD|title::LABEL|type::TYPE|value::DEFAULT|api::URL|columns::COLS;;

Supported types:
Type	Renders
text	Text input
number	Numeric input
password	Password input
checkbox	Checkbox (value 1 or 0)
radio	Radio group
date-box	Custom calendar picker (outputs YYYY-MM-DD)
data-combo	Dropdown that fetches from an API endpoint
find-object-box	Modal picker for large lists (paginated + searchable)
map-box	Leaflet map. Click to pick lat/lng (name is "latField,lngField")
current-user	Hidden input, auto-filled with the logged-in user's code

Example with a lookup:
text

|name::warehouse_id|title::Warehouse|type::find-object-box|value::|api::/warehousePickerApi|columns::Code,Title;;

Example with coordinates:
text

|name::warehouse_lat,warehouse_long|title::Location|type::map-box|value::;;

Custom Elements

Every UI pattern ships as a Web Component. They load as ES modules with no build step.
Element	Purpose
<fetch-data-table>	Full CRUD table: search, pagination, modals, API wiring, permission-aware buttons
<data-combo>	Dropdown that fetches options from a URL
<find-object-box>	Modal picker for large datasets
<date-box>	Custom calendar control
<map-box>	Leaflet map. write mode picks a location; read mode draws OSRM routes
<jali-form>	Standalone form element (used by Login)

All elements:

    Style themselves via a <link> inside a Shadow DOM

    Read config from HTML attributes

    Emit standard change, submit, and custom events

    Work identically inside and outside modals

<fetch-data-table> attributes
html

<fetch-data-table
    api="/warehouseApi"
    table-id="warehouseTable"
    caption="Warehouses"
    classes="blue neon pastel"
    inputs="..."
    columns="Title, Location"
    readonly                          <!-- optional: hides + disables everything -->
    action-buttons="add,edit,delete"  <!-- optional: subset of "add,edit,delete" or "none" -->
    extra-buttons="Approve:/approveApi, Deny:/denyApi"  <!-- optional custom buttons -->
    data-AccessCode="101">
</fetch-data-table>

<map-box> modes

Write mode — user clicks to pick a coordinate:
html

<map-box mode="write" height="320px" value="35.6892,51.3890"></map-box>

Read mode — draws an OSRM driving route between two or more points:
html

<map-box mode="read" points="35.6892,51.3890;32.6546,51.6680" height="400px"></map-box>

Dispatches route-ready with {distance, duration, polyline}.
RBAC

Jali Frame's auth model is object-code-based, not role-based. It's simple and it's enforced everywhere.
How it works

Every page has an object code. Every API request checks the user's permission on that code. Every HTML element with data-AccessCode="N" is removed server-side if the user lacks READ.
The permission matrix
text

USER_CODE | OBJECT_CODE | CAN_READ | CAN_CREATE | CAN_UPDATE | CAN_DELETE
----------|-------------|----------|------------|------------|------------
1         | 101         | 1        | 1          | 1          | 1
2         | 101         | 1        | 0          | 0          | 0
2         | 102         | 1        | 0          | 0          | 0

Rows are auto-synced on every boot:

    New objects inserted into SYS_OBJECTS

    Cross-joined with all users to create missing rows

    ADMIN (USER_CODE = 1) force-granted all flags via finalizeRegistration()

Server-side HTML trimming

This is the framework's signature feature. Consider:
html

<div class="menu-item" data-AccessCode="101" data-route="/warehouse">
    Warehouse Management
</div>

If the user lacks READ on object 101, pageHandlerOpener.filterHtmlByAccess removes this entire block from the HTML response. The user's browser never sees it. No flicker, no bypass.
Directory Structure
text

jali-frame/
├── backend/
│   ├── ConfigAndLauncherManager/
│   │   └── readConfig.java               # Config file + GUI/console + HTTP bootstrap
│   ├── DataBase/
│   │   ├── dataBaseManager.java          # Connection pool with atomic availability flags
│   │   ├── dataBaseUtils.java            # isAllowed, isAuthenticated, JSON serialization
│   │   ├── DataBaseInit.java             # Self-bootstrapping schema + stored procedures
│   │   └── GenerateGenericSQLQuery.java  # SQL builders (Read / Update / Create / Delete)
│   ├── InterFaces/
│   │   ├── CrudQueries.java
│   │   ├── JaliFiles.java
│   │   └── JaliWebPage.java
│   ├── PageRelatedEnums/
│   │   ├── FilesEnum.java                # Every static asset
│   │   ├── WebPagesEnum.java             # Every page route + object code
│   │   ├── CrudQueriesEnum.java          # Every SQL query
│   │   └── FileTypesEnum.java
│   ├── WebServerHandlers/
│   │   ├── pageHandlerOpener.java        # Serves files + trims by data-AccessCode
│   │   ├── apiManagement.java            # Generic CRUD handler
│   │   └── webServerUtils.java           # JSON parser, cookie extractor, URL parser
│   └── mainServerLaunch.java             # Wires APIs + routes at boot
│
└── ClientSide/
    ├── FrameWorksLib/
    │   ├── Jali.js/
    │   │   ├── core.js                   # Imports all custom elements
    │   │   └── custom_elements/
    │   │       ├── dataTable.js
    │   │       ├── dataCombo.js
    │   │       ├── findObjectBox.js
    │   │       ├── dateBox.js
    │   │       ├── mapBox.js
    │   │       └── jaliForm.js
    │   ├── JaliFrame.css/
    │   │   ├── readDataTable.css
    │   │   ├── dataCombo.css
    │   │   ├── FindObjectBox.css
    │   │   ├── dateBox.css
    │   │   ├── dataForm.css
    │   │   └── mapBox.css
    │   └── Leaflet/                      # Bundled Leaflet for offline use
    └── [Your pages]/

Configuration

config.txt is loaded from the JAR directory or the working directory:
Key	Description	Default
BASE_FILE_ADDRESS	Absolute path to ClientSide/	computed
server	SQL Server host	localhost
port	SQL Server port	1433
databaseName	Database name	PROJECT_ZERO
username / password	SQL credentials	sa / 12
MAX_CONNECTION_POOL	Pool size	5
portNumber	HTTP port	8080
serverIP	HTTP bind address	127.0.0.1
queueWaitLine	HTTP backlog	10
MAX_SESSION_TIME	Cookie lifetime (seconds)	86400

Rebuild the derived URL and base URL automatically whenever a value changes.
Database Conventions

Jali Frame expects these conventions:
Convention	Reason
Soft delete via is_deleted BIT	The Delete builder emits UPDATE ... SET is_deleted = 1
Primary keys named <table>_id	Read queries treat the first column as the row ID
Lookup tables use <x>_code + <x>_title	FOB and combo assume first column is value, second is display
Audit tables have no is_deleted	Framework falls back to hard delete
Row order in setColumnNames(...) = order of ? params	Positional payloads

If your existing schema violates these, you can still use Jali Frame — just override the Delete builder with a raw SQL string (see the AccessRequestHeader example in any app built with Jali).
Architecture
text

┌──────────────────────────────────────────────────────────┐
│                    Browser (ES Modules)                  │
│                                                          │
│  <fetch-data-table>  <data-combo>  <map-box>  <date-box> │
│           │               │           │         │        │
│           └───────────────┴───────────┴─────────┘        │
│                         │                                │
│                         ▼  fetch() JSON                  │
└──────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────┼────────────────────────────────┐
│                         ▼                                │
│  com.sun.net.httpserver.HttpServer                       │
│                                                          │
│  ┌──────────────────┐        ┌───────────────────────┐   │
│  │ pageHandlerOpener│        │ apiManagement.dataGen │   │
│  │  • serve file    │        │  • GET/POST/PUT/DELETE│   │
│  │  • trim by code  │        │  • auth + permissions │   │
│  │  • rewrite base  │        │  • SQL execution      │   │
│  └──────────────────┘        └───────────────────────┘   │
│                                                          │
│  ┌───────────────────────────────────────────────────┐   │
│  │  Enum layer                                       │   │
│  │    FilesEnum        → static assets               │   │
│  │    WebPagesEnum     → routes + object codes       │   │
│  │    CrudQueriesEnum  → every SQL query             │   │
│  └───────────────────────────────────────────────────┘   │
│                                                          │
│  ┌───────────────────────────────────────────────────┐   │
│  │  DataBase layer                                   │   │
│  │    dataBaseManager  → connection pool             │   │
│  │    dataBaseUtils    → helpers (JSON, auth)        │   │
│  │    DataBaseInit     → self-bootstrapping schema   │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
                    SQL Server

Extending the Framework
Add a new custom element

    Create ClientSide/FrameWorksLib/Jali.js/custom_elements/yourThing.js

    Define a class extending HTMLElement, register with customElements.define

    Add an entry in FilesEnum

    Register the route in mainServerLaunch

    Import it in core.js

    Use it in any HTML page

Add a new SQL builder

Extend GenerateGenericSQLQuery with a static inner class. The existing builders (ReadQuery, UpdateQuery, CreateQuery, DeleteQuery) are the reference.
Add an auth strategy

The framework ships with cookie-based sessions. To swap it, replace webServerUtils.extractTokenFromCookie and the IS_AUTHENTICATE stored procedure. The framework itself doesn't care — it only calls isAuthenticated(token).
Known Limitations

Honestly documented, not hidden:

    Positional JSON payloads. Create/Update requests send {input0, input1, …} in enum column order. Reordering form inputs without updating the enum breaks the save silently. Named fields are on the roadmap.

    FOB/combo display doesn't re-hydrate on edit. The value saves correctly, but the visual display resets to blank on edit.

    Boot sequence has two phases. Objects queue during route registration and flush at finalizeRegistration(). Calling it is required; forgetting it means no permissions get created.

    No build step for JS. Which is a feature — until you want tree-shaking or bundling. Roadmap includes an optional esbuild pipeline.

    No WebSocket support. Tables refresh via fetch. Live updates are on the roadmap.

    com.sun.net.httpserver is used instead of Netty. Fine for internal tools, not for high-concurrency public services.

Roadmap

v2.0 — ergonomics

    □

    Named payload fields ({title: "...", code: 42}) instead of {input0, input1}
    □

    Split overloaded element attributes (name → field + api)
    □

    Single-pass boot sequence
    □

    Element re-hydration on edit
    □

    Auto-generated TypeScript definitions for custom elements

v2.1 — platform

    □

    Optional esbuild pipeline for bundling and tree-shaking
    □

    WebSocket support for live-updating tables
    □

    Plugin system for custom HTTP handlers
    □

    Migrate to Netty (HTTP/2 + streaming)

v2.2 — DX

    □

    CLI: jali new page Warehouse --table warehouse
    □

    Auto-generate HTML from enum entry
    □

    Dev mode with hot reload

Contributing

Issues and PRs are welcome. Before opening a PR:

    Keep the framework dependency-free — Jali's value is its small surface

    Any new element must work inside and outside modals, with and without Shadow DOM

    Any new SQL builder must produce parameterized queries only

    Document breaking changes in the PR description

License

MIT — use it, fork it, sell it, whatever. Attribution appreciated but not required.
<p align="center"> <em>Because application structure should be data, not boilerplate.</em> </p>
