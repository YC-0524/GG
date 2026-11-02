# Game Gang

Finding teammates usually means posting in random Discord servers and hoping someone at your level replies. Game Gang fixes that. You build a profile listing the games you play and your current rank in each, and other players can search for a game to see everyone who plays it, along with their rank.
Game Gang is a web app for finding people to play with the game of same interest. Game Gang also provides a filtering system, allowing users who are more of casual players can find those to chill and play.


Built for CMPT 276 by Group 17 with Spring Boot, Thymeleaf and PostgreSQL, and deployed on Render.

- **Live app:** https://gg-dnjn.onrender.com
- **Repository:** https://github.com/YC-0524/GG


## How it works

1. **Sign up** with your email, or log in with... options in one click.
2. **Build your game list.** Search for a game, pick your current rank if it asks for one, and add it. Update your rank as you climb, or remove games you've stopped playing.
3. **Show it off.** Your profile shows every game on your list with your rank, so other players know what you play before they reach out.
4. **Find players.** Search by game to see everyone who plays it and their rank, or search by username to find a friend directly.


### User roles

| Role | Can do |
| --- | --- |
| `USER` | Manage their own game list, view profiles, search for players/games |
| `MODERATOR` | Everything a user can, plus moderator tools (`/dashboard/moderator`) |
| `ADMIN` | Everything a moderator can, plus admin tools and the list of all users (`/dashboard/admin`) |

New accounts are created as `USER`.


## Running it locally

You need JDK 17 or newer. Maven isn't required, since the project includes the Maven wrapper.

```bash
git clone https://github.com/YC-0524/GG.git
cd GG
cp .env.example .env
```

Fill in the Discord values in `.env` if you want Discord login. Email login works without them, but the four `DISCORD_*` variables must exist, even if empty. Then load the file and start the app:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

Open http://localhost:8080. Locally the app uses an in-memory database, so accounts reset every time it restarts.

To run the tests:

```bash
./mvnw test
```


## Deployment

The app deploys to Render from `render.yaml`, which sets up the web service and a PostgreSQL database together. Set the `DISCORD_*` environment variables in the Render dashboard, and every push to `main` redeploys.



## Project structure

```
src/main/java/com/group17/game/
├── Config/        # Spring Security configuration
├── Controllers/   # Auth, Dashboard and Profile routes
├── DTOs/          # Discord API response objects
├── Models/        # JPA entities (User, LoginConnection) and repositories
└── Utilities/     # Password hashing
src/main/resources/
├── templates/     # Thymeleaf pages (login, register, dashboard, admin, moderator, profile)
├── static/        # CSS and JavaScript
├── application.properties
└── application-prod.properties
src/test/java/...  # Controller and model tests
```

## Main routes

| Route | Page |
| --- | --- |
| `/` | Home |
| `/auth/register` | Sign-up form |
| `/auth/login` | Login form (email or Discord) |
| `/auth/discord` | Starts Discord login |
| `/dashboard` | Dashboard for the logged-in role |
| `/dashboard/moderator` | Moderator tools |
| `/dashboard/admin` | Admin tools |
| `/profile/me`, `/profile/{id}` | Profile pages |


## Team

| Name | GitHub |
| --- | --- |
| Yena Choi| [@YC-0524](https://github.com/YC-0524) |
| Cameron Armstrong | [@CKAY-9](https://github.com/CKAY-9) |
| Jean Eskut | [@jeaneskut](https://github.com/jeaneskut) |
| Taehyun Lee | [@khonshuin](https://github.com/khonshuin) |
| Sorosh Zarei | [@Sorosh-2026](https://github.com/Sorosh-2026) |
