# Game Gang

Game Gang is a privacy-first matchmaking web app that helps gamers find compatible teammates, in the style of a dating app. Finding teammates usually means posting in random Discord servers and hoping someone at your level replies. On Game Gang, you browse player cards, see what each person plays and how well, and like the ones who look like a good match.

Profiles are low-pressure by design. There are no real names or personal photos: players use their Discord avatar and an in-game or preferred nickname. Each profile shows the games they play with their current rank, and the plan is to fill this in automatically from Steam, Discord and Riot.

The goal is to match players by how they like to play, not just what they play. Custom tags, playstyle preferences and age-range filters will help each player find the right group, so casual players looking to relax can find each other as easily as competitive players chasing a rank.

Game Gang is also planned to have an SOS hub. Players stuck in a single-player game can ask high-playtime veterans for tips, and players who need guidance in a multiplayer game can reach out to experts.

Built for CMPT 276 by Group 17 with Spring Boot, Thymeleaf and PostgreSQL, and deployed on Render.

- **Live app:** https://gg-dnjn.onrender.com
- **Repository:** https://github.com/YC-0524/GG

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Project structure](#project-structure)
- [Routes](#routes)
- [Open issues and known limitations](#open-issues-and-known-limitations)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Team](#team)

## Features

### Available now

- Sign up and log in with email and password, or with Discord. Sessions persist between visits. Players who sign up through Discord are asked to set a password on their first login.
- Game list: Add games from the catalogue with your current rank, update the rank as you climb, and remove games you've stopped playing. The catalogue currently has Apex Legends, League of Legends, Minecraft, Overwatch 2, Rocket League and Valorant, each with its own ranks.
- Profiles: Every player has a public profile showing their game list. You can search for players by username.
- Player cards: The main dashboard shows other players one at a time, and you can like them.
- Moderation: Moderators can time players out, reset an offensive username or avatar, and ask an admin to suspend someone.
- Administration: Admins can see every account, suspend and unsuspend players, approve or deny suspension requests, and permanently delete accounts.

### Roles

| Role | Can do |
| --- | --- |
| `USER` | Manage their own game list, view profiles, search for players, like players |
| `MODERATOR` | Everything a user can, plus the moderator tools at `/dashboard/moderator` |
| `ADMIN` | Everything a moderator can, plus the admin tools and the list of all users at `/dashboard/admin` |

New accounts start as `USER`. Staff accounts are created from environment variables (see [Configuration](#configuration)).

## Architecture

Game Gang is a server-rendered Spring Boot app. Controllers handle each request, read or write the database through Spring Data JPA, and return a Thymeleaf page. The admin and moderator controllers also check the user's role first. There is no separate frontend app or public API.

| Layer | What it does | Where |
| --- | --- | --- |
| Security | Keeps the login session and sends users who still need to set a password to that page | `Config/SecurityConfig`, `Config/PasswordResetFilter` |
| Controllers | One per area of the site. Admin and moderator routes check the user's role through `AccessService`. | `Controllers/` |
| Domain | Users, roles and status, game entries, likes, login connections, suspension requests, password flows | `Models/` |
| Utilities | Role checks, the game and rank catalogue, password hashing | `Utilities/` |
| Views | Server-rendered pages and their styles and scripts | `resources/templates/`, `resources/static/` |
| Startup | Creates staff accounts on boot (dev test accounts locally, env-driven accounts on Render) | `Config/DevAccountSeeder`, `Config/StaffAccountSeeder` |

### Tech stack

| Part | Technology |
| --- | --- |
| Language | Java 17 (the Docker image runs Java 21) |
| Framework | Spring Boot 4.1 with Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator and Spring Shell |
| Views | Thymeleaf, HTML, CSS, JavaScript |
| Database | PostgreSQL in production, H2 in memory for local development and tests |
| Login | Email and password, Discord OAuth2 |
| Hosting | Docker on Render, set up by `render.yaml` |

## Getting started

### Prerequisites

- JDK 17 or newer
- Git

You don't need to install Maven, because the project includes the Maven wrapper (`./mvnw`). You don't need a database either, because local runs use an in-memory H2 database.

### Install and run

```bash
git clone https://github.com/YC-0524/GG.git
cd GG
cp .env.example .env
```

Fill in the Discord values in `.env` if you want Discord login to work locally (see [Configuration](#configuration)). Email login works without them, but the four `DISCORD_*` variables must exist, even if they're empty, or the app won't start.

Load the file and start the app:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

Open http://localhost:8080. The in-memory database is wiped every time the app restarts, so accounts you create locally don't persist.

### Test staff accounts

To log in as an admin or moderator locally, start the app with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

This creates two accounts on startup, both with the password `gg-dev-password`:

- `admin@gg.test`
- `moderator@gg.test`

These accounts only exist in the local in-memory database. They are never created on Render.

### Run the tests

Load your `.env` first, because the tests start the whole app and need the `DISCORD_*` variables to exist:

```bash
set -a && source .env && set +a
./mvnw test
```

## Configuration

All configuration comes from environment variables. Locally, they come from `.env`; on Render they're set in the dashboard.

| Variable | Needed for | Notes |
| --- | --- | --- |
| `DISCORD_CLIENT_ID` | Discord login | From the Discord Developer Portal, under OAuth2 |
| `DISCORD_CLIENT_SECRET` | Discord login | Same page. Resetting it there breaks the old one immediately. |
| `DISCORD_REDIRECT_URI` | Discord login | Must exactly match a redirect listed in the Developer Portal, e.g. `https://gg-dnjn.onrender.com/auth/discord/callback` |
| `DISCORD_OAUTH_URI` | Discord login | The full authorize URL from Discord's URL Generator, with the `identify` and `email` scopes and the same redirect |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Production only | Filled in automatically by `render.yaml` from the Render database |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Production only | Creates the admin account on startup. Password must be at least 8 characters. |
| `MODERATOR_EMAIL`, `MODERATOR_PASSWORD` | Production only | Optional. Creates a moderator account the same way. |
| `PORT` | Optional | Port the app listens on. Defaults to 8080. |

The staff seeder never changes an account that already exists. If the email you choose is already registered, it logs a warning and skips it, so pick an email nobody has signed up with.

## Deployment

The app is deployed on Render as a Docker web service with a PostgreSQL database, both defined in `render.yaml`.

1. In Render, create a new **Blueprint** from this repository. Render reads `render.yaml` and creates the `game` web service and the `game-db` database, and connects the `DB_*` variables between them.
2. In the `game` service's **Environment** settings, set the four `DISCORD_*` variables and `ADMIN_EMAIL` / `ADMIN_PASSWORD`, and the moderator pair if you want one.
3. In the Discord Developer Portal, add your Render URL followed by `/auth/discord/callback` as an OAuth2 redirect.

Render builds the `Dockerfile`, which compiles the app with Maven and runs the jar. `render.yaml` sets `SPRING_PROFILES_ACTIVE=prod`, which switches the app to PostgreSQL. Every push to `main` redeploys. Render checks `/actuator/health` to confirm the app started.

The service is on Render's free plan, so it sleeps when idle. The first request after a quiet period can take about a minute.

## Project structure

```
src/main/java/com/group17/game/
├── Commands/      # Spring Shell command for changing a user's role (local use)
├── Config/        # Security, the password-reset filter, web config and the account seeders
├── Controllers/   # Auth, Dashboard, Games, Profile, PlayerLike, Admin and Moderator routes
├── DTOs/          # Discord API responses and view objects (player cards, user summaries)
├── Models/        # JPA entities, enums and repositories
├── Resolvers/     # Injects the logged-in user into controller methods
├── Utilities/     # Role checks, the game catalogue and password hashing
└── GameApplication.java
src/main/resources/
├── templates/     # Thymeleaf pages
├── static/        # CSS, JavaScript and images
├── application.properties
└── application-prod.properties
src/test/java/com/group17/game/
├── Commands/      # Role command tests
├── Controllers/   # Controller tests for each area
└── Models/        # Entity tests
```

## Routes

| Route | Page or action |
| --- | --- |
| `/` | Home page |
| `/auth/register`, `/auth/login` | Sign-up and login forms (email or Discord) |
| `/auth/discord`, `/auth/discord/callback` | Discord login and its return address |
| `/auth/set-password` | Set a password (required after a first Discord login) |
| `/dashboard` | Dashboard for the logged-in role, with player cards |
| `/dashboard/players/{id}/like` | Like a player |
| `/games` | Your game list: add, edit and remove games |
| `/profile/me`, `/profile/{id}` | Your profile and other players' profiles |
| `/profile/search` | Search players by username |
| `/dashboard/moderator` | Moderator tools: timeout, clean profile, request suspension |
| `/dashboard/admin` | Admin tools: all users, suspend, unsuspend, review requests, delete |

## Open issues and known limitations

**Open pull requests**

- [#12 Google sign-in](https://github.com/YC-0524/GG/pull/12): needs updating to the current `main`.
- [#26 Role badge](https://github.com/YC-0524/GG/pull/26): saved for iteration 2.

**Known limitations**

- **No search by game yet.** You can search players by username, but not by the games they play.
- **No filters yet.** The dashboard's filter panel says "Filters are coming soon" and doesn't filter anything.
- **Only six games.** The catalogue is a fixed list in `Utilities/GameCatalogue`, and adding a game means changing the code.
- **Discord login errors** show Spring's default error page instead of a message on the login page.
- **Password security:** The only rule is a minimum of 8 characters, and passwords are hashed with SHA-256 without a salt.
- **Role command:** The `user-upgrade` shell command only works where you can type into the running app, which Render's free plan doesn't allow.
- **Local data** is lost on restart because local runs use an in-memory database.

To report a bug or suggest a feature, [open an issue](https://github.com/YC-0524/GG/issues).

## Roadmap

### Iteration 2: finding teammates by game

- **Game search** with game icons, and a page for each game listing the players who chose to be found through it
- **Tags and filters:** play style, genre, schedule, mic preference and group size, plus online, AFK and offline status
- **Game requests:** a "Game not found" page that lets players ask for a game, reviewed by moderators
- **More logins:** Google, Steam, and possibly Apple
- **Account details:** stronger password rules, a forgotten-password flow and clearer login errors

### Later

- **Automatic profiles:** import game libraries, favourite genres and competitive ranks from Steam, Discord and Riot instead of entering them by hand
- **Age-range filters** so players can find groups around their own age
- **SOS hub:** a community space where players ask high-playtime veterans for tips in single-player games, or experts for guidance in multiplayer games

## Contributing

1. Branch from `main` with a name that says what the work is, such as `feature/game-search` or `fix/login-error`.
2. Keep each pull request to one feature or fix. In the description, say what the branch is for and which files and folders it adds or changes.
3. Run `./mvnw test` before opening the pull request, and get a review from a teammate before merging.

## Team

Built by Group 17 for CMPT 276.

| Name | GitHub |
| --- | --- |
| Yena Choi | [@YC-0524](https://github.com/YC-0524) |
| Cameron Armstrong | [@CKAY-9](https://github.com/CKAY-9) |
| Jean Eskut | [@jeaneskut](https://github.com/jeaneskut) |
| Taehyun Lee | [@khonshuin](https://github.com/khonshuin) |
| Sorosh Zarei | [@Sorosh-2026](https://github.com/Sorosh-2026) |
