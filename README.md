# Spring Boot Markdown Note-taking App

> A simple note-taking app that uses Markdown for formatting notes.

## Table of Contents

- [General Info](#general-information)
- [Technologies Used](#technologies-used)
- [Features](#features)
- [Setup](#setup)
- [Usage](#usage)
- [Project Status](#project-status)
- [Acknowledgements](#acknowledgements)
- [License](#license)

## General Information

This is a simple note-taking application built with Spring Boot. It allows users to save the note, upload Markdown files, 
check the grammar, and render it in HTML. The application uses an in-memory database for storing note metadata and local 
storage for storing the Markdown files. It provides a RESTful API for interacting with the notes.

## Technologies Used

- Java 21.0.11
- Spring Boot 4.0.6
- H2 Database
- LanguageTool API
- Flexmark Java Library

## Features

- Create and save notes with Markdown content.
- Upload Markdown files to create notes.
- Check grammar of the Markdown content using LanguageTool API.
- Render Markdown content to HTML using Flexmark Java Library.

## Setup

To run this project locally, you'll need Java 21 or higher. Follow these steps to set up and run the application:

1. Clone the repository:

   ```bash
   git clone https://github.com/krisnaajiep/springboot-markdown-note-taking-app
   ```

2. Navigate to the project directory:

   ```bash
   cd springboot-markdown-note-taking-app
   ```
   
3. Build the project using Maven:

   ```bash
   mvn clean install
   ```
   
4. Run the application:

   ```bash
   java -jar target/springboot-markdown-note-taking-app-1.0.0.jar
   ```
   
## Usage

Once the application is running, you can interact with it using the following API endpoints:

- `POST /notes`: Upload a Markdown file and save the metadata.
- `GET /notes/check`: Check the grammar of a note's Markdown content with required `filename` parameter.
- `GET /notes`: Retrieve all notes metadata.
- `GET /notes/render`: Render the Markdown content of a note to HTML with required `filename` parameter.

API documentation:

- [**Swagger UI**](https://krisnaajiep.github.io/springboot-markdown-note-taking-app/)
- [**OpenAPI Specification**](https://github.com/krisnaajiep/springboot-markdown-note-taking-app/blob/dev/docs/openapi.yaml)

## Project Status

Project is: _complete_.

[![CI](https://github.com/krisnaajiep/springboot-markdown-note-taking-app/actions/workflows/maven.yml/badge.svg)](https://github.com/krisnaajiep/springboot-markdown-note-taking-app/actions/workflows/maven.yml)

## Acknowledgements

This project was inspired by [roadmap.sh](https://roadmap.sh/projects/markdown-note-taking-app).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.