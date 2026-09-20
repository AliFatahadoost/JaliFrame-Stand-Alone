# Jali Frame

### A from-scratch Java full-stack framework for database-backed applications.

Jali Frame turns database-backed application structure into declarations.

Instead of building controllers, services, repositories, DTOs, mappers, and a separate frontend for every CRUD screen, you define your application through a small set of enums and use Jali's Web Components to render it.

**No Spring. No ORM. No React. No frontend build step.**

Just Java, SQL Server, HTML, CSS, JavaScript, and a framework built around them.

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019%2B-blue)](https://www.microsoft.com/sql-server)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## What is Jali Frame?

Jali Frame is a convention-driven full-stack Java framework for building database-backed web applications.

The framework's central idea is simple:

> **Application structure should be declared, not repeatedly implemented.**

A single `CrudQueriesEnum` entry can describe a database object's CRUD operations and permission boundary:

```java
Warehouse(
    new ReadQuery()
        .setTableName("warehouse")
        .setColumnNames(
            "warehouse_id",
            "warehouse_title",
            "warehouse_code",
            "warehouse_lat",
            "warehouse_long"
        )
        .getQuery(),

    new UpdateQuery()
        .setTableName("warehouse")
        .setColumnNames(
            "warehouse_title",
            "warehouse_code",
            "warehouse_lat",
            "warehouse_long"
        )
        .setWhereQuery("warehouse_id = ?")
        .getQuery(),

    new CreateQuery()
        .setTableName("warehouse")
        .setColumnNames(
            "warehouse_title",
            "warehouse_code",
            "warehouse_lat",
            "warehouse_long"
        )
        .getQuery(),

    "UPDATE warehouse SET is_deleted = 1 WHERE warehouse_id = ?",

    101
)
```

Then expose it in HTML:

```html
<fetch-data-table
    api="/warehouseApi"
    inputs="
        |name::warehouse_title|title::Title|type::text|value::;;
        |name::warehouse_lat,warehouse_long|title::Location|type::map-box|value::;;
    "
    columns="Title, Location">
</fetch-data-table>
```

That is enough to create a working CRUD interface with:

* Search
* Pagination
* Create
* Edit
* Soft delete
* Permission-aware actions
* Validation
* Modal forms
* Interactive map input
* Authentication
* Authorization
* JSON API handling

You describe the application.

Jali Frame handles the plumbing.

---

# Why Jali Frame?

Traditional Java web applications often turn a simple database-backed screen into a chain of abstractions:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
DTO
    ↓
Mapper
    ↓
REST API
    ↓
Frontend API layer
    ↓
Frontend component
```

Jali Frame takes a different approach:

```text
Database
    ↓
CrudQueriesEnum
    ↓
Generic API
    ↓
Web Component
```

A new CRUD page is intentionally small:

```text
CrudQueriesEnum
WebPagesEnum
FilesEnum
HTML
```

The framework handles the repetitive work around them.

Jali Frame is deliberately opinionated.

It does not attempt to be a universal replacement for every Java web stack. It is designed for applications where SQL-backed business data and CRUD interfaces are the core of the system.

Typical examples include:

* ERP modules
* Inventory systems
* Internal management software
* Administrative panels
* Database-first applications
* CRUD-heavy enterprise tools

---

# Core Concepts

## 1. Object Codes

Every application object receives a numeric object code.

For example:

```text
101 = Warehouse
102 = Products
103 = Employees
```

The same object code is used throughout the framework:

```text
WebPagesEnum
CrudQueriesEnum
HTML
```

For example:

```html
<div
    class="menu-item"
    data-AccessCode="101"
    data-route="/warehouse">
    Warehouse Management
</div>
```

The number becomes the permission boundary for that object.

One code connects:

```text
Page
 ↓
API
 ↓
SQL
 ↓
Permissions
```

---

## 2. Enums Declare the Application

Jali Frame uses a small set of enums to describe the application's structure.

| Enum              | Purpose                             |
| ----------------- | ----------------------------------- |
| `FileTypesEnum`   | MIME types                          |
| `FilesEnum`       | Static files and HTML pages         |
| `WebPagesEnum`    | Routes, pages, object codes         |
| `CrudQueriesEnum` | CRUD queries and permission binding |

Instead of spreading application metadata across controllers, annotations, repositories, and configuration classes, Jali keeps it explicit.

---

## 3. Generic CRUD APIs

Jali Frame generates generic HTTP CRUD handlers around `CrudQueriesEnum`.

The API layer handles:

```text
GET
POST
PUT
DELETE
```

along with:

* Authentication
* Permission checks
* URL parameter parsing
* JSON body parsing
* Parameter binding
* SQL execution
* Result serialization

Jali does not try to hide the database.

It removes the repetitive HTTP plumbing around database operations.

---

## 4. Positional Payloads

Create and update requests currently use positional fields:

```json
{
    "input0": "...",
    "input1": "...",
    "input2": "..."
}
```

The order comes from:

```java
.setColumnNames(...)
```

For example:

```java
.setColumnNames(
    "warehouse_title",
    "warehouse_code",
    "warehouse_lat"
)
```

must correspond to the HTML input order.

Named payloads are planned for a future release.

---

# Quick Start

Jali Frame is designed so the first startup does **not** require manually creating `config.txt`.

The launcher handles configuration for you.

## Requirements

* Java 17+
* SQL Server 2019+
* A modern web browser

No Node.js or frontend build system is required at runtime.

---

## 1. Start Jali Frame

Launch the Jali Frame server:

```bash
java -cp target/jali-frame.jar yourpackage.mainServerLaunch
```

On startup, Jali Frame opens its launcher/configuration flow.

You will be presented with:

```text
Y → Launch the configuration GUI
n → Use the CLI
s → Silent boot
```

The launcher is the normal way to configure the framework.

---

# Configuration Launcher

## GUI Mode

Choose:

```text
Y
```

to open the configuration GUI.

The GUI lets you configure the application's:

* SQL Server connection
* Database name
* Database username
* Database password
* HTTP server address
* HTTP server port
* Connection pool size
* Session lifetime
* Client-side base path
* HTTP queue/backlog settings

The launcher writes these settings to `config.txt`.

You do **not** need to manually create or edit the configuration file just to get Jali Frame running.

---

## CLI Mode

Choose:

```text
n
```

to use the command-line configuration interface.

The CLI provides commands such as:

```text
launch
set
testdb
```

This allows you to configure and test the environment directly from the terminal.

For example, the launcher can be used to:

```text
configure → save → test database → launch server
```

without manually editing configuration files.

---

## Silent Mode

Choose:

```text
s
```

for silent startup.

Silent mode skips the interactive configuration process and immediately starts Jali Frame using the existing configuration.

This is intended for environments where the configuration has already been created.

---

# Manual Configuration

`config.txt` can also be edited manually when required.

The file is stored next to the JAR or in the working directory.

A typical configuration looks like:

```properties
BASE_FILE_ADDRESS=/absolute/path/to/ClientSide

server=localhost
port=1433
databaseName=PROJECT_ZERO

username=sa
password=your_password

MAX_CONNECTION_POOL=5

portNumber=8080
serverIP=127.0.0.1
queueWaitLine=10

MAX_SESSION_TIME=86400
```

Manual configuration is useful for automation, deployment scripts, containers, or environments where configuration is managed outside the launcher.

For normal development, the launcher GUI or CLI is the intended workflow.

---

## Configuration Reference

| Key                   | Description                    | Default        |
| --------------------- | ------------------------------ | -------------- |
| `BASE_FILE_ADDRESS`   | Absolute path to `ClientSide/` | Computed       |
| `server`              | SQL Server host                | `localhost`    |
| `port`                | SQL Server port                | `1433`         |
| `databaseName`        | Database name                  | `PROJECT_ZERO` |
| `username`            | SQL username                   | `sa`           |
| `password`            | SQL password                   | `12`           |
| `MAX_CONNECTION_POOL` | Maximum database connections   | `5`            |
| `portNumber`          | HTTP server port               | `8080`         |
| `serverIP`            | HTTP bind address              | `127.0.0.1`    |
| `queueWaitLine`       | HTTP backlog                   | `10`           |
| `MAX_SESSION_TIME`    | Session lifetime in seconds    | `86400`        |

The launcher also rebuilds derived URLs and base paths when related configuration values change.

> **Security:** change default development credentials before deploying outside a local development environment.

---

# First Boot

Once configuration is complete, Jali Frame initializes its internal structures.

It creates schemas such as:

```text
USERS_DATA_AND_PERMISSIONS
INIT_DATABASE
```

and system tables such as:

```text
SYS_USERS
SYS_OBJECTS
OBJECT_USER_PERMISSION
```

Authentication and permission stored procedures are installed as part of initialization.

During startup, application objects are registered and synchronized with the permission system.

The registration process is finalized with:

```java
DataBaseInit.finalizeRegistration();
```

---

# Open the Application

Once the server has started:

```text
http://127.0.0.1:8080/
```

Log in and begin using the application.

---

# Building Your First Page

Let's build a `Book` page.

## Step 1 — Create the Table

```sql
CREATE TABLE dbo.book (
    book_id     NUMERIC(18,0) IDENTITY(1,1) PRIMARY KEY,
    book_title  NVARCHAR(100) NOT NULL,
    book_author NVARCHAR(50) NOT NULL,
    book_year   NUMERIC(18,0) NULL,
    is_deleted  BIT NULL DEFAULT 0
);
```

---

## Step 2 — Declare the CRUD Operations

In `CrudQueriesEnum.java`:

```java
Book(
    new ReadQuery()
        .setTableName("book")
        .setColumnNames(
            "book_id",
            "book_title",
            "book_author",
            "book_year"
        )
        .setColumnDataTypes(
            "numeric",
            "nvarchar(100)",
            "nvarchar(50)",
            "numeric"
        )
        .setWhereQuery("AND isnull(is_deleted, 0) = 0")
        .getQuery(),

    new UpdateQuery()
        .setTableName("book")
        .setColumnNames(
            "book_title",
            "book_author",
            "book_year"
        )
        .setWhereQuery("book_id = ?")
        .getQuery(),

    new CreateQuery()
        .setTableName("book")
        .setColumnNames(
            "book_title",
            "book_author",
            "book_year"
        )
        .getQuery(),

    "UPDATE book SET is_deleted = 1 WHERE book_id = ?",

    500
)
```

`500` is the object's permission code.

---

## Step 3 — Register the File

In `FilesEnum.java`:

```java
Book(
    "/Books/Book.html",
    false,
    FileTypesEnum.html
)
```

---

## Step 4 — Register the Page

In `WebPagesEnum.java`:

```java
Book(
    500,
    FilesEnum.Book,
    "/book"
)
```

---

## Step 5 — Register the API

In `mainServerLaunch.java`:

```java
server.createContext(
    "/bookApi",
    new apiManagement.dataApiGen.builder()
        .shouldAuthenticate(true)
        .sendTokenToDB(false)
        .setQuery(CrudQueriesEnum.Book)
        .build()
);

WebPagesEnum.Book.registerRoute(server);
```

At the end of startup:

```java
DataBaseInit.finalizeRegistration();
```

---

## Step 6 — Create the HTML

Create:

```text
ClientSide/Books/Book.html
```

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">

    <title>Books</title>

    <script
        type="module"
        src="http://127.0.0.1:8080/coreJs">
    </script>
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
```

Restart Jali Frame and navigate to:

```text
/book
```

You now have a complete database-backed CRUD page.

---

# What You Get

That configuration produces:

```text
                         /book
                           │
                           ▼
                  <fetch-data-table>
                           │
             ┌─────────────┼─────────────┐
             ▼             ▼             ▼
            GET           POST       PUT / DELETE
             │             │             │
             └─────────────┼─────────────┘
                           ▼
                     Generic API
                           │
                     Permission check
                           │
                           ▼
                   CrudQueriesEnum
                           │
                           ▼
                      SQL Server
```

The table component provides:

* Search
* Pagination
* Create modal
* Edit modal
* Delete actions
* Validation
* API integration
* Permission-aware controls

The backend provides:

* HTTP routing
* Authentication
* Authorization
* JSON handling
* Parameter binding
* SQL execution
* Static file serving
* Server-side HTML access filtering

---

# The Input DSL

Every form field is represented by a `;;`-terminated block:

```text
|name::FIELD|title::LABEL|type::TYPE|value::DEFAULT;;
```

Example:

```html
inputs="
    |name::book_title|title::Title|type::text|value::;;
    |name::book_year|title::Year|type::number|value::;;
"
```

## Supported Input Types

| Type              | Purpose                     |
| ----------------- | --------------------------- |
| `text`            | Text input                  |
| `number`          | Numeric input               |
| `password`        | Password input              |
| `checkbox`        | Boolean input (`1` / `0`)   |
| `radio`           | Radio group                 |
| `date-box`        | Custom calendar picker      |
| `data-combo`      | API-backed dropdown         |
| `find-object-box` | Searchable object picker    |
| `map-box`         | Interactive map             |
| `current-user`    | Hidden logged-in user value |

---

## Lookup Example

```html
|name::warehouse_id
|title::Warehouse
|type::find-object-box
|value::
|api::/warehousePickerApi
|columns::Code,Title;;
```

---

## Coordinate Example

```html
|name::warehouse_lat,warehouse_long
|title::Location
|type::map-box
|value::;;
```

---

# Custom Elements

Jali Frame provides reusable Web Components loaded as ES modules.

No frontend build system is required.

| Element              | Purpose                  |
| -------------------- | ------------------------ |
| `<fetch-data-table>` | Full CRUD table          |
| `<data-combo>`       | API-backed dropdown      |
| `<find-object-box>`  | Searchable object picker |
| `<date-box>`         | Calendar/date control    |
| `<map-box>`          | Interactive map          |
| `<jali-form>`        | Standalone form          |

Components are designed to work inside and outside modal dialogs.

They encapsulate their own markup and styling using standard browser APIs such as Shadow DOM.

---

# `<fetch-data-table>`

Example:

```html
<fetch-data-table
    api="/warehouseApi"
    table-id="warehouseTable"
    caption="Warehouses"
    classes="blue neon pastel"
    inputs="..."
    columns="Title, Location"
    readonly
    action-buttons="add,edit,delete"
    extra-buttons="Approve:/approveApi, Deny:/denyApi"
    data-AccessCode="101">
</fetch-data-table>
```

Available configuration includes:

```text
api
table-id
caption
classes
inputs
columns
readonly
action-buttons
extra-buttons
data-AccessCode
```

`action-buttons` supports:

```text
add
edit
delete
none
```

---

# `<map-box>`

## Write Mode

Select a location interactively:

```html
<map-box
    mode="write"
    height="320px"
    value="35.6892,51.3890">
</map-box>
```

---

## Read Mode

Display an OSRM route:

```html
<map-box
    mode="read"
    points="
        35.6892,51.3890;
        32.6546,51.6680
    "
    height="400px">
</map-box>
```

The component emits:

```text
route-ready
```

with:

```javascript
{
    distance,
    duration,
    polyline
}
```

---

# Authentication and Permissions

Jali Frame uses an object-code permission model.

Each object can define:

```text
CAN_READ
CAN_CREATE
CAN_UPDATE
CAN_DELETE
```

Example:

```text
USER_CODE | OBJECT_CODE | CAN_READ | CAN_CREATE | CAN_UPDATE | CAN_DELETE
----------|-------------|----------|------------|------------|-----------
1         | 101         |    1     |     1      |     1      |     1
2         | 101         |    1     |     0      |     0      |     0
2         | 102         |    1     |     0      |     0      |     0
```

Permissions are enforced by the backend.

Hiding a button in JavaScript is not an authorization mechanism.

The API checks the user's permissions before allowing the requested operation.

---

# Server-Side HTML Trimming

Jali Frame can also remove unauthorized HTML before the response reaches the browser.

Example:

```html
<div
    class="menu-item"
    data-AccessCode="101"
    data-route="/warehouse">

    Warehouse Management

</div>
```

When the user lacks `READ` permission for object `101`, the server removes that element from the generated HTML response.

The flow becomes:

```text
Permission check
       ↓
Unauthorized element removed
       ↓
HTML sent to browser
```

This prevents unauthorized UI elements from being delivered to the client.

API authorization still remains the actual security boundary.

---

# Permission Synchronization

During startup, registered objects are synchronized with the permission model.

Jali Frame:

1. Inserts new objects into `SYS_OBJECTS`.
2. Creates missing user/object permission rows.
3. Grants the administrator full permissions.
4. Finalizes registration.

The finalization step is:

```java
DataBaseInit.finalizeRegistration();
```

---

# Directory Structure

```text
jali-frame/
│
├── backend/
│   │
│   ├── ConfigAndLauncherManager/
│   │   └── readConfig.java
│   │
│   ├── DataBase/
│   │   ├── dataBaseManager.java
│   │   ├── dataBaseUtils.java
│   │   ├── DataBaseInit.java
│   │   └── GenerateGenericSQLQuery.java
│   │
│   ├── InterFaces/
│   │   ├── CrudQueries.java
│   │   ├── JaliFiles.java
│   │   └── JaliWebPage.java
│   │
│   ├── PageRelatedEnums/
│   │   ├── FilesEnum.java
│   │   ├── WebPagesEnum.java
│   │   ├── CrudQueriesEnum.java
│   │   └── FileTypesEnum.java
│   │
│   ├── WebServerHandlers/
│   │   ├── pageHandlerOpener.java
│   │   ├── apiManagement.java
│   │   └── webServerUtils.java
│   │
│   └── mainServerLaunch.java
│
└── ClientSide/
    │
    ├── FrameWorksLib/
    │   │
    │   ├── Jali.js/
    │   │   ├── core.js
    │   │   └── custom_elements/
    │   │       ├── dataTable.js
    │   │       ├── dataCombo.js
    │   │       ├── findObjectBox.js
    │   │       ├── dateBox.js
    │   │       ├── mapBox.js
    │   │       └── jaliForm.js
    │   │
    │   ├── JaliFrame.css/
    │   │   ├── readDataTable.css
    │   │   ├── dataCombo.css
    │   │   ├── FindObjectBox.css
    │   │   ├── dateBox.css
    │   │   ├── dataForm.css
    │   │   └── mapBox.css
    │   │
    │   └── Leaflet/
    │
    └── [Application Pages]
```

---

# Database Conventions

Jali Frame follows several conventions.

| Convention                  | Purpose                               |
| --------------------------- | ------------------------------------- |
| `is_deleted BIT`            | Soft-delete convention                |
| `<table>_id`                | Primary key convention                |
| `<x>_code` + `<x>_title`    | Lookup convention                     |
| First selected column       | Treated as row identifier             |
| `setColumnNames(...)` order | Determines positional parameter order |

The default delete operation is:

```sql
UPDATE table_name
SET is_deleted = 1
WHERE table_id = ?
```

Applications using existing schemas can provide custom delete SQL when they do not follow the standard convention.

---

# Architecture

Jali Frame is built around Java's standard HTTP server:

```text
┌─────────────────────────────────────────────────────────┐
│                       Browser                           │
│                                                         │
│  <fetch-data-table>  <data-combo>  <map-box>          │
│            │              │             │               │
│            └──────────────┴─────────────┘               │
│                           │                             │
│                         fetch()                         │
└───────────────────────────┼─────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              com.sun.net.httpserver.HttpServer         │
│                                                         │
│  ┌────────────────────┐   ┌─────────────────────────┐  │
│  │ pageHandlerOpener  │   │ apiManagement           │  │
│  │                    │   │                         │  │
│  │ • static files     │   │ • GET                   │  │
│  │ • HTML trimming    │   │ • POST                  │  │
│  │ • route handling   │   │ • PUT                   │  │
│  │                    │   │ • DELETE                │  │
│  └────────────────────┘   └─────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Enum Layer                                         │  │
│  │                                                   │  │
│  │ FilesEnum       → files                           │  │
│  │ WebPagesEnum    → routes + object codes           │  │
│  │ CrudQueriesEnum → SQL + permissions               │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Database Layer                                    │  │
│  │                                                   │  │
│  │ dataBaseManager → connection pool                │  │
│  │ dataBaseUtils   → database helpers               │  │
│  │ DataBaseInit    → schema + procedures             │  │
│  └───────────────────────────────────────────────────┘  │
└───────────────────────────┼─────────────────────────────┘
                            │
                            ▼
                       SQL Server
```

The framework intentionally uses:

```java
com.sun.net.httpserver.HttpServer
```

to keep the backend small and dependency-light.

---

# Extending Jali Frame

## Add a Custom Element

Create:

```text
ClientSide/FrameWorksLib/Jali.js/custom_elements/yourThing.js
```

Then:

1. Extend `HTMLElement`.
2. Register the component with `customElements.define(...)`.
3. Add the required file to `FilesEnum`.
4. Import it from `core.js`.
5. Use the element in HTML.

Example:

```javascript
class YourThing extends HTMLElement {

    connectedCallback() {
        // component initialization
    }

}

customElements.define("your-thing", YourThing);
```

---

## Add a SQL Builder

Extend:

```text
GenerateGenericSQLQuery
```

The existing builders provide the pattern:

```text
ReadQuery
UpdateQuery
CreateQuery
DeleteQuery
```

Generated SQL should use parameterized values.

---

## Add an Authentication Strategy

Jali Frame currently uses cookie-based sessions.

The authentication boundary is intentionally small.

The implementation can be replaced through:

```text
webServerUtils.extractTokenFromCookie
```

and the authentication procedure:

```text
IS_AUTHENTICATE
```

The rest of the framework interacts with authentication through the authentication check.

---

# Known Limitations

Jali Frame intentionally keeps its core small, and that means there are some sharp edges.

## Positional Payloads

Current requests use:

```json
{
    "input0": "...",
    "input1": "..."
}
```

Changing input order without updating the corresponding query definition can break data binding.

Named fields are planned.

---

## Lookup Re-hydration

`find-object-box` and `data-combo` currently have limitations when restoring their display values during edit operations.

The underlying value is preserved, but the visual display may not always be restored.

---

## Two-Phase Boot

Application objects are registered and finalized separately.

```java
DataBaseInit.finalizeRegistration();
```

is currently required.

A single-pass startup flow is planned.

---

## No JavaScript Build Pipeline

Jali Frame loads ES modules directly.

This provides a very small frontend toolchain:

```text
HTML
CSS
JavaScript
```

with no mandatory bundler or package manager.

The trade-off is that there is currently no built-in production bundling or tree-shaking pipeline.

---

## No WebSockets

Live data synchronization is not currently built in.

Tables refresh through standard HTTP requests.

---

## HTTP Server Choice

Jali Frame uses:

```java
com.sun.net.httpserver.HttpServer
```

This keeps the framework lightweight and understandable, but Jali Frame is not designed as a high-concurrency public internet application server.

---

# Roadmap

## v2.0 — Ergonomics

* [ ] Named request fields
* [ ] Split overloaded component attributes
* [ ] Single-pass boot sequence
* [ ] Reliable component re-hydration
* [ ] Automatic TypeScript definitions

## v2.1 — Platform

* [ ] Optional esbuild pipeline
* [ ] WebSocket support
* [ ] Plugin system for HTTP handlers
* [ ] Optional Netty backend
* [ ] HTTP/2 support

## v2.2 — Developer Experience

* [ ] Jali CLI
* [ ] `jali new page Warehouse --table warehouse`
* [ ] Generate HTML from enum definitions
* [ ] Development mode
* [ ] Hot reload

---

# Design Philosophy

### SQL is not the enemy.

Jali Frame does not attempt to hide SQL behind an ORM.

The database remains a first-class part of the application.

### CRUD should not require boilerplate.

When a screen is fundamentally:

```text
SELECT
INSERT
UPDATE
DELETE
```

the HTTP and UI plumbing should not overwhelm the actual business logic.

### The browser already provides a frontend platform.

Modern browsers already support:

* Custom Elements
* ES Modules
* Shadow DOM
* Fetch
* HTML
* CSS

Jali Frame builds directly on those capabilities.

### Authorization belongs on the server.

A hidden button is not security.

Jali checks permissions at the API boundary and can additionally remove unauthorized HTML before delivery.

### Keep the framework understandable.

Jali Frame intentionally has a limited surface area.

There is no giant abstraction stack hiding what happens between the browser and SQL Server.

---

# Who Is Jali Frame For?

Jali Frame is intended for developers building:

* Internal enterprise applications
* ERP systems
* Inventory systems
* Administrative tools
* Database-backed management software
* CRUD-heavy business applications
* Small-team Java applications
* Database-first applications

It is less suited to projects that require:

* Very high connection counts
* Large distributed systems
* A large frontend framework ecosystem
* Advanced reactive infrastructure
* Database-vendor independence
* A full enterprise framework ecosystem

Jali Frame deliberately solves a narrower problem:

> **How much code should it take to turn a SQL-backed business object into a usable web application?**

---

# Project Philosophy in One Example

A conventional CRUD feature might involve:

```text
Controller
Service
Repository
Entity
DTO
Mapper
REST endpoint
Frontend API layer
Frontend table
Frontend form
Permission checks
Route configuration
```

Jali Frame aims to reduce that to:

```text
CrudQueriesEnum
WebPagesEnum
FilesEnum
HTML
```

The framework fills in the repetitive plumbing.

---

# Contributing

Issues and pull requests are welcome.

Please preserve the framework's core principles:

* Keep the core dependency-light.
* Keep custom elements usable inside and outside modals.
* Keep SQL builders parameterized.
* Document breaking changes.
* Avoid abstractions that add complexity without solving a real Jali Frame problem.

---

# License

MIT License.

Use it, fork it, modify it, ship it, sell it.

Attribution is appreciated but not required.

---

<p align="center">
    <em>Because application structure should be data, not boilerplate.</em>
</p>
