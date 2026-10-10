# Evil SMP HasteTools (Paper 26.2)

This plugin creates tagged event-reward tools that apply Haste V while held in the **main hand**. Haste V uses potion amplifier `4` (amplifiers start at zero). The effect is refreshed periodically with an overlapping duration to avoid flickering. When the player stops holding the tagged tool, the plugin removes the Haste effect it tracked as having applied.

## Commands
- `/hashtools axe [player]` — give a Haste V axe.
- `/hashtools pickaxe [player]` — give a Haste V pickaxe.
- `/hashtools reload` — reload `config.yml`.

Permission: `hashtools.admin` (operators by default).

## Install/build
This is the complete plugin **source project**, not a precompiled JAR. Paper 26.2 uses Java 25. To build it, install JDK 25 and Maven, then run `mvn package` from this folder. The JAR will be created at `target/HasteTools-1.0.0.jar`. Put that JAR in the server's `plugins` folder and restart the server.

## Configure
Edit `src/main/resources/config.yml` before building, or edit `plugins/HasteTools/config.yml` after the first run. `effect-amplifier: 4` means Haste V. Set `hide-particles: true` to hide particles. Item materials, names and lore can be changed in the config.

## Notes
- The plugin only recognizes its own tagged tools; renaming a normal tool in an anvil does not grant the effect.
- Haste is applied while the tagged tool is held in the main hand, not merely carried in inventory.
- Test on a backup/test server first, especially if other plugins manage potion effects.


## Easiest build method (no Java/Maven install on your computer)

This project includes a GitHub Actions workflow that builds the plugin using Java 25.

1. Create a new repository on GitHub.
2. Upload the contents of this folder to the repository's `main` branch (make sure `.github/workflows/build.yml` is included).
3. Open the repository's **Actions** tab and select **Build HasteTools**.
4. If prompted, enable GitHub Actions, then click **Run workflow**.
5. Open the completed workflow run and download the **HasteTools-jar** artifact.
6. Extract the downloaded artifact ZIP. It contains `HasteTools-1.0.0.jar`. Upload that `.jar` to the server's `plugins` folder while the server is stopped, then start the server.

The build uses Java 25 and the Paper API version range for Minecraft 26.2 or later. The artifact must still be tested on your exact server build before relying on it for an event.
