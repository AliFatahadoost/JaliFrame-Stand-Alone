# Jali Frame

### A from-scratch Java full-stack framework for database-backed applications.

Jali Frame turns database-backed application structure into declarations.

Instead of building controllers, services, repositories, DTOs, mappers, route handlers, and a separate frontend for every CRUD screen, you define your application through a small set of enums and use Jali's Web Components to render it.

**No Spring. No ORM. No React. No build step.**

Just Java, SQL Server, HTML, CSS, JavaScript, and a framework built around them.

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019%2B-blue)](https://www.microsoft.com/sql-server)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## What does Jali Frame actually do?

A single `CrudQueriesEnum` entry can define a table's CRUD behavior and permission boundary:

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

Then expose it with a single Web Component:

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

That gives you a working CRUD surface with:

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
* Object-level authorization
* JSON API handling

The application code describes **what exists**.

Jali Frame handles **how it works**.

---

# Why Jali Frame?

Traditional Java web stacks are powerful, but for internal business software they can introduce a lot of machinery before the first useful screen exists.

A simple database table can turn into:

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
REST Endpoint
    ↓
Frontend API Layer
    ↓
Frontend Component
```

Jali Frame takes a different approach.

```text
Database
    ↓
CrudQueriesEnum
    ↓
Generic API
    ↓
<fetch-data-table>
```

Application structure is treated as **declarative data** rather than repetitive application code.

A new CRUD page is intentionally small:

1. Define its queries.
2. Assign an object code.
3. Register the page.
4. Add a `<fetch-data-table>`.

The framework handles the repetitive plumbing.

Jali Frame is deliberately opinionated. It is not trying to replace every Java web stack.

It is designed for applications where:

* SQL Server is the database.
* CRUD operations are a major part of the application.
* You want server-side Java without a large framework stack.
* You want a browser UI without React or another frontend framework.
* You want a small, understandable codebase.
* You care about shipping internal business software quickly.

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

The same code becomes the permission boundary throughout the application.

It is used in:

```java
WebPagesEnum
CrudQueriesEnum
```

and in HTML:

```html
<div data-AccessCode="101">
    Warehouse Management
</div>
```

One object code represents one authorization boundary.

That means the framework can connect:

```text
Page
  ↓
API
  ↓
SQL
  ↓
Permissions
```

without requiring a separate role/controller/permission system for every screen.

---

## 2. Enums Declare the Application

Jali Frame uses a small number of enums to describe the application.

| Enum              | Purpose                               |
| ----------------- | ------------------------------------- |
| `FileTypesEnum`   | MIME types                            |
| `FilesEnum`       | Static files and HTML pages           |
| `WebPagesEnum`    | Routes, pages, object codes           |
| `CrudQueriesEnum` | SQL operations and permission binding |

Instead of scattering application metadata throughout annotations, controllers, and configuration files, Jali keeps the application's structure explicit.

---

## 3. Generic CRUD APIs

Jali Frame can generate CRUD HTTP handlers from a `CrudQueriesEnum` entry.

The generic API handles:

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
* SQL parameter binding
* Result serialization

The goal is not to hide SQL.

The goal is to remove the repetitive HTTP plumbing around it.

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

The order is determined by:

```java
.setColumnNames(...)
```

Therefore:

```java
.setColumnNames(
    "warehouse_title",
    "warehouse_code",
    "warehouse_lat"
)
```

must correspond to the order of the HTML inputs.

This is intentionally simple, but it is also one of Jali Frame's biggest current limitations.

Named payloads are planned.

---

# Quick Start

## Requirements

* Java 17+
* SQL Server 2019+
* A modern web browser

Jali Frame does not require Node.js, npm, React, Maven at runtime, or a Java application server.

---

## 1. Clone

```bash
git clone https://github.com/yourname/jali-frame.git
cd jali-frame
```

---

## 2. Configure

Create `config.txt` next to the JAR or in the working directory:

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

### Configuration

| Key                   | Description                    | Default        |
| --------------------- | ------------------------------ | -------------- |
| `BASE_FILE_ADDRESS`   | Absolute path to `ClientSide/` | Computed       |
| `server`              | SQL Server host                | `localhost`    |
| `port`                | SQL Server port                | `1433`         |
| `databaseName`        | Database name                  | `PROJECT_ZERO` |
| `username`            | SQL username                   | `sa`           |
| `password`            | SQL password                   | `12`           |
| `MAX_CONNECTION_POOL` | Maximum DB connections         | `5`            |
| `portNumber`          | HTTP server port               | `8080`         |
| `serverIP`            | HTTP bind address              | `127.0.0.1`    |
| `queueWaitLine`       | HTTP backlog                   | `10`           |
| `MAX_SESSION_TIME`    | Session lifetime in seconds    | `86400`        |

> **Security:** change the default credentials before using Jali Frame outside a local development environment.

Derived URLs and base paths are recalculated automatically when relevant configuration values change.

---

## 3. Boot

```bash
java -cp target/jali-frame.jar yourpackage.mainServerLaunch
```

Jali Frame supports three startup modes:

```text
Y  → launch the configuration GUI
n  → console mode
s  → silent boot
```

Console mode provides commands such as:

```text
launch
set
testdb
```

---

## 4. Database Initialization

On first startup Jali Frame can initialize its internal database structures.

It creates:

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

Required authentication and permission stored procedures are installed automatically.

Objects registered during startup are synchronized with the permission matrix during:

```java
DataBaseInit.finalizeRegistration();
```

---

## 5. Open the Application

```text
http://127.0.0.1:8080/
```

---

# Building Your First Page

Let's build a `Book` page.

## Step 1 — Create the table

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

## Step 2 — Declare the CRUD operations

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

## Step 3 — Register the file

In `FilesEnum.java`:

```java
Book(
    "/Books/Book.html",
    false,
    FileTypesEnum.html
)
```

---

## Step 4 — Register the page

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

## Step 6 — Build the page

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

Rebuild and restart:

```bash
mvn clean compile

java -cp target/jali-frame.jar yourpackage.mainServerLaunch
```

Navigate to:

```text
/book
```

You now have a database-backed CRUD page.

---

# What You Get

That small configuration produces:

```text
                    /book
                      │
                      ▼
             <fetch-data-table>
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
        GET          POST        PUT/DELETE
          │           │           │
          └───────────┼───────────┘
                      ▼
               Generic API
                      │
                Permission
                   check
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
* Input validation
* API integration
* Permission-aware controls

The framework provides:

* HTTP routing
* Authentication
* Authorization
* JSON handling
* Parameter binding
* SQL execution
* HTML serving
* Server-side access filtering

---

# The Input DSL

Form inputs are declared through the `inputs` attribute.

Each field is a `;;`-terminated block:

```text
|name::FIELD|title::LABEL|type::TYPE|value::DEFAULT;;
```

For example:

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
| `checkbox`        | Boolean value (`1` / `0`)   |
| `radio`           | Radio group                 |
| `date-box`        | Custom date picker          |
| `data-combo`      | API-backed dropdown         |
| `find-object-box` | Searchable modal picker     |
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

## Map Example

```html
|name::warehouse_lat,warehouse_long
|title::Location
|type::map-box
|value::;;
```

The map component writes latitude and longitude into the two specified fields.

---

# Custom Elements

Jali Frame provides a small library of Web Components.

They are loaded as ES modules and require no frontend build pipeline.

| Element              | Purpose                   |
| -------------------- | ------------------------- |
| `<fetch-data-table>` | Full CRUD data table      |
| `<data-combo>`       | API-backed dropdown       |
| `<find-object-box>`  | Searchable object picker  |
| `<date-box>`         | Calendar/date input       |
| `<map-box>`          | Interactive map           |
| `<jali-form>`        | Standalone form component |

All components are designed to work both inside and outside modal dialogs.

Components use Shadow DOM for encapsulated markup and styling where appropriate.

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

Useful attributes include:

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

`action-buttons` accepts subsets such as:

```text
add
edit
delete
none
```

---

# `<map-box>`

`map-box` has two primary modes.

## Write Mode

Used to select a location:

```html
<map-box
    mode="write"
    height="320px"
    value="35.6892,51.3890">
</map-box>
```

The user selects a coordinate on the map.

---

## Read Mode

Used to display an OSRM driving route:

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

The component dispatches:

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

Jali Frame bundles Leaflet for offline map rendering.

---

# Authentication and RBAC

Jali Frame uses an object-code permission model.

Every object can define:

```text
CAN_READ
CAN_CREATE
CAN_UPDATE
CAN_DELETE
```

A simplified permission matrix looks like:

```text
USER_CODE | OBJECT_CODE | CAN_READ | CAN_CREATE | CAN_UPDATE | CAN_DELETE
----------|-------------|----------|------------|------------|-----------
1         | 101         |    1     |     1      |     1      |     1
2         | 101         |    1     |     0      |     0      |     0
2         | 102         |    1     |     0      |     0      |     0
```

Permissions are enforced at the API level.

That matters because hiding a button in JavaScript is not authorization.

The server checks the permission before executing the requested operation.

---

# Server-Side HTML Trimming

Jali Frame also applies permissions to HTML before the page reaches the browser.

For example:

```html
<div
    class="menu-item"
    data-AccessCode="101"
    data-route="/warehouse">

    Warehouse Management

</div>
```

If the authenticated user does not have `READ` permission for object `101`, the server removes that element before sending the response.

This provides:

```text
No permission
      ↓
Element removed
      ↓
HTML sent to browser
```

rather than:

```text
HTML sent
      ↓
JavaScript hides element
```

This is a UI access-control mechanism.

It does **not** replace API authorization; the API still enforces permissions independently.

---

# Permission Synchronization

During startup Jali Frame synchronizes registered objects with the permission model.

The initialization process:

1. Inserts new objects into `SYS_OBJECTS`.
2. Creates missing user/object permission rows.
3. Grants full permissions to `ADMIN`.
4. Finalizes the registration phase.

The final step is:

```java
DataBaseInit.finalizeRegistration();
```

Forgetting this call means newly registered permissions are not finalized.

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

| Convention               | Purpose                               |
| ------------------------ | ------------------------------------- |
| `is_deleted BIT`         | Soft-delete convention                |
| `<table>_id`             | Primary key convention                |
| `<x>_code` + `<x>_title` | Lookup convention                     |
| First selected column    | Treated as the row identifier         |
| `setColumnNames()` order | Determines positional parameter order |

The default delete builder generates:

```sql
UPDATE table_name
SET is_deleted = 1
WHERE table_id = ?
```

Tables that do not use the standard convention can provide their own delete SQL.

This allows existing databases to be integrated without requiring schema rewrites.

---

# Architecture

Jali Frame is built around Java's standard HTTP server:

```text
┌─────────────────────────────────────────────────────────┐
│                       Browser                           │
│                                                         │
│   <fetch-data-table>   <data-combo>   <map-box>        │
│             │               │             │             │
│             └───────────────┴─────────────┘             │
│                             │                           │
│                         fetch()                         │
└─────────────────────────────┼───────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│              com.sun.net.httpserver.HttpServer         │
│                                                         │
│  ┌─────────────────────┐  ┌──────────────────────────┐ │
│  │ pageHandlerOpener   │  │ apiManagement            │ │
│  │                     │  │                          │ │
│  │ • static files      │  │ • GET                    │ │
│  │ • HTML filtering    │  │ • POST                   │ │
│  │ • route handling    │  │ • PUT                    │ │
│  │                     │  │ • DELETE                 │ │
│  └─────────────────────┘  └──────────────────────────┘ │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Enum Layer                                         │  │
│  │                                                   │  │
│  │ FilesEnum      → files                            │  │
│  │ WebPagesEnum   → routes + permissions             │  │
│  │ CrudQueriesEnum→ SQL + permissions                │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Database Layer                                    │  │
│  │                                                   │  │
│  │ dataBaseManager → connection pool                │  │
│  │ dataBaseUtils   → database helpers               │  │
│  │ DataBaseInit    → schema + procedures             │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────┼───────────────────────────┘
                              │
                              ▼
                        SQL Server
```

The framework intentionally avoids a heavyweight HTTP runtime.

For internal applications, Jali Frame uses:

```java
com.sun.net.httpserver.HttpServer
```

instead of an application server such as Tomcat or a networking framework such as Netty.

---

# Extending Jali Frame

Jali Frame is designed to be extended without changing its core philosophy.

## Add a Custom Element

Create:

```text
ClientSide/FrameWorksLib/Jali.js/custom_elements/yourThing.js
```

Then:

1. Extend `HTMLElement`.
2. Register it with `customElements.define(...)`.
3. Add it to `FilesEnum`.
4. Import it in `core.js`.
5. Use it from any HTML page.

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

The existing builders are the reference implementation:

```text
ReadQuery
UpdateQuery
CreateQuery
DeleteQuery
```

All generated SQL should use parameterized values.

---

## Add an Authentication Strategy

Jali Frame currently uses cookie-based sessions.

The authentication boundary is intentionally small.

Replace:

```text
webServerUtils.extractTokenFromCookie
```

and the authentication stored procedure:

```text
IS_AUTHENTICATE
```

The rest of the framework interacts with authentication through the authentication check rather than depending on the implementation.

---

# Known Limitations

Jali Frame is intentionally small, and that comes with trade-offs.

## Positional JSON Payloads

Current payloads use:

```json
{
    "input0": "...",
    "input1": "..."
}
```

The HTML input order must match the SQL column order.

Named fields are planned.

---

## Lookup Re-hydration

`find-object-box` and `data-combo` currently do not always restore their display value correctly when an existing record is opened for editing.

The database value itself is preserved; the issue is visual re-hydration.

---

## Two-Phase Boot

Objects are registered during startup and finalized later.

```java
DataBaseInit.finalizeRegistration();
```

is required.

A future release will simplify registration into a single boot phase.

---

## No JS Build Pipeline

Jali's JavaScript is loaded directly as ES modules.

That means:

* No npm requirement
* No bundler required
* No compilation step

It also means there is currently no built-in tree-shaking or production bundling pipeline.

An optional esbuild pipeline is planned.

---

## No WebSockets

Tables currently refresh through standard HTTP requests.

There is no built-in real-time synchronization layer.

---

## HTTP Server Choice

Jali Frame uses:

```java
com.sun.net.httpserver.HttpServer
```

This keeps the framework small and dependency-light, but it is not intended to compete with high-concurrency application stacks for public internet-facing services.

---

# Roadmap

## v2.0 — Ergonomics

* [ ] Named request fields
* [ ] Split overloaded component attributes
* [ ] Single-pass boot sequence
* [ ] Reliable component re-hydration on edit
* [ ] Automatic TypeScript definitions for custom elements

## v2.1 — Platform

* [ ] Optional esbuild pipeline
* [ ] WebSocket support
* [ ] Plugin system for custom HTTP handlers
* [ ] Optional Netty backend
* [ ] HTTP/2 support

## v2.2 — Developer Experience

* [ ] Jali CLI
* [ ] `jali new page Warehouse --table warehouse`
* [ ] HTML generation from enum declarations
* [ ] Development mode
* [ ] Hot reload

---

# Design Philosophy

Jali Frame is built around a few deliberately strong opinions.

### SQL is not the enemy.

Jali does not try to hide the database behind an ORM abstraction.

You define SQL through the framework's query builders and conventions.

### CRUD should not require boilerplate.

If a screen is fundamentally:

```text
SELECT
INSERT
UPDATE
DELETE
```

you should not need hundreds of lines of application plumbing to expose it.

### The browser is already a platform.

Modern browsers already provide:

* Custom Elements
* ES Modules
* Shadow DOM
* Fetch
* HTML forms
* CSS

Jali uses those primitives directly instead of introducing a mandatory frontend framework.

### Authorization belongs on the server.

UI visibility is useful.

It is not authorization.

Jali therefore enforces permissions at the API boundary and can additionally trim inaccessible HTML before it reaches the browser.

### Small systems are easier to understand.

Jali Frame intentionally has a limited surface area.

There is no dependency mountain to climb before understanding what the framework is doing.

---

# Who Is Jali Frame For?

Jali Frame is particularly suited to:

* Internal enterprise applications
* Inventory systems
* ERP modules
* Administrative panels
* Management software
* CRUD-heavy business applications
* Database-first systems
* Small teams that want a compact Java stack

It is less appropriate when you need:

* A public-scale distributed backend
* Massive concurrent connections
* A large frontend ecosystem
* Advanced reactive infrastructure
* Vendor-independent database abstraction
* A full enterprise framework ecosystem

Jali Frame does not try to solve every web development problem.

It solves a narrower one:

> **How much code should it take to turn a SQL-backed business object into a usable web application?**

---

# Project Philosophy in One Example

A conventional implementation might require:

```text
Controller
Service
Repository
Entity
DTO
Mapper
REST endpoint
Frontend API code
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

The framework takes care of the repetitive plumbing between them.

---

# Contributing

Issues and pull requests are welcome.

When contributing, please keep the framework's core principles intact:

* Keep the core dependency-light.
* Keep custom elements usable inside and outside modals.
* Keep SQL builders parameterized.
* Document breaking changes.
* Avoid adding abstractions that exist only to solve problems the framework does not have.

---

# License

MIT License.

Use it, fork it, modify it, ship it, sell it.

Attribution is appreciated but not required.

---

<p align="center">
    <em>Because application structure should be data, not boilerplate.</em>
</p>
