# Atlas Browser

> Simple web browser built with JavaFX.


![Atlas Browser Image](atlas-logo.png)

## Features

Atlas comes equipped with essential browsing features wrapped in a clean, flat design:

* **UI:** Clean interface styled with CSS and **FontAwesome** icons (via Ikonli).
* **Tabbed Browsing:** Dynamic tabs. Open, close, and switch between multiple sites.
* **Download Manager:** Detects file downloads automatically and saves them in the background using multi-threading.
* **Context Menu:** Custom right-click menu to open links in new tabs, copy URLs, or copy text.
* **History:** Keeps track of visited pages (per session).
* **Zoom** Full control over zoom.
* **Settings:** Customizable homepage.

## Tech Stack

* **Language:** Java 21+
* **Framework:** JavaFX (Controls, Web, FXML)
* **Build Tool:** Maven
* **Libraries:**
    * `org.openjfx:javafx-web` (WebKit engine)
    * `org.kordamp.ikonli` (Vector icons)

## Project Structure

The project follows the standard Maven architecture:

* `src/main/resources`: Contains the FXML views, CSS styles, and icons.
* `src/main/java`: Contains the Controller logic and Application entry point.

## Motivation

This project was created for educational purposes.