# Snipster

A lightweight, in-browser (soon to also be in-app) text editor that allows editing the same text snippets across multiple devices/sessions.

Currently [live in early open beta](https://snipster.live/)

## Running Locally

###

Installation: check out the code. Add a file to the root directory called ".env", fill with environment variables as necessary. See examples from existing environments. Sign into Heroku CLI account on machine.

Starting up: from the root directory, run './gradlew build' and `./gradlew stage` to build and stage the project for Heroku, and then run the relevant command to start Heroku locally:

- On Linux: `heroku local`
- On Windows: `heroku local -f Procfile.windows`

The app should start up and be viewable at http://localhost:5000/

The 2 procfiles are necessary because Heroku needs to be told how to start the app locally, and it needs the correct path format for each environment.
