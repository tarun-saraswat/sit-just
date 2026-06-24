# Environment Setup Guide

This folder contains scripts to set up your local environment for running SIT tests against an ephemeral environment.

---

## Prerequisites

- Python 3 installed
- `coast` CLI installed and authenticated
- AWS CLI installed
- IntelliJ IDEA with the **EnvFile** plugin (see Step 4)

---

## Step 1 — Configure Your Environment Name

Open `env-setup/configs.py` and update `ENV_NAME` to the ephemeral environment you want to test against:

```python
ENV_NAME = "env084"   # <-- change this to your env (e.g. "env091")
```

---

## Step 2 — Generate Environment Variables & VM Options

Run the main script from inside the `env-setup/` directory:

```bash
cd env-setup
python3 ephemeralEnvVars.py
```

**What this does:**
1. Creates `src/main/resources/services-configuration.xml` from the template (if it doesn't exist — this file is gitignored and must exist locally)
2. Calls `coast` to find the running instance in your ephemeral env
3. Downloads the `eph.properties` file from the instance via S3
4. Parses the properties and generates two files in the `env-setup/` folder:
   - `envVariables-<ENV_NAME>` — all environment variables for the run config
   - `vmOptions-<ENV_NAME>` — Kafka and environment JVM `-D` flags
5. Creates convenience symlinks `envVariables` and `vmOptions` pointing to the above

> **Note:** DB usernames are replaced with `root` and passwords with `password123` automatically. AWS credential keys are stripped from the output file.

---

## Step 3 — Set Up AWS Credentials

AWS session credentials expire periodically. Use `makeEnvFile.py` to refresh them quickly.

### How to get credentials from AWS

1. Log in to the AWS console
2. Click your account name (top-right) → **"Access Keys"** or go to the SSO portal
3. Under **"Option 1: Set AWS environment variables"**, click **Copy** — this copies three `export` statements to your clipboard:
   ```
   export AWS_ACCESS_KEY_ID="ASIA..."
   export AWS_SECRET_ACCESS_KEY="..."
   export AWS_SESSION_TOKEN="..."
   ```

### Run the script

With the credentials still in your clipboard, run:

```bash
python3 env-setup/makeEnvFile.py
```

This writes `env-setup/credentials.env` with the three keys. The file is git-ignored and will never be committed.

---

### Create a `makeenv` alias (recommended)

Instead of typing the full path every time, add an alias to your shell profile.

**For zsh** — add to `~/.zshrc`:
```bash
alias makeenv="python3 /path/to/sit-just/env-setup/makeEnvFile.py"
```

**For bash** — add to `~/.bashrc` or `~/.bash_profile`:
```bash
alias makeenv="python3 /path/to/sit-just/env-setup/makeEnvFile.py"
```

Replace `/path/to/sit-just` with the actual absolute path to your local clone. Then reload your shell:
```bash
source ~/.zshrc   # or source ~/.bashrc
```

Now you can refresh credentials any time by copying them from AWS and running:
```bash
makeenv
```

---

## Step 4 — Configure IntelliJ Run Configuration

### 4a — Install / Update the EnvFile Plugin

1. Open IntelliJ → **Settings** (`⌘,` on Mac)
2. Go to **Plugins** → **Marketplace**
3. Search for **"EnvFile"**
4. If not installed → click **Install**
5. If already installed → go to **Installed** tab, find EnvFile, and click **Update** if available
6. Restart IntelliJ when prompted

### 4b — Create or Edit a Run Configuration

1. In IntelliJ, go to **Run** → **Edit Configurations…**
2. Click **+** to add a new configuration (TestNG or JUnit depending on your test runner), or select an existing one
3. Set the following fields:
   - **Name:** something descriptive, e.g. `SIT - env084`
   - **Test kind:** Suite / Class / Method as needed
   - **Working directory:** the root of the `sit-just` project

### 4c — Add VM Options

In the **VM options** field, paste the contents of `env-setup/vmOptions-<ENV_NAME>`, e.g.:

```
-DKAFKA_TXN_HA_PRIMARY=...
-DKAFKA_TXN_PRIMARY=...
-Denvironment=env084
```

Or click the **Modify options** dropdown → enable **"VM options"** if it isn't visible.

### 4d — Add Environment Files via EnvFile

1. In the run configuration window, click the **EnvFile** tab (added by the plugin)
2. Make sure **"Enable EnvFile"** checkbox is ticked
3. Click **+** → **"..."** (browse) and add **both** files:
   - `env-setup/envVariables-<ENV_NAME>` (or the `envVariables` symlink)
   - `env-setup/credentials.env`
4. Ensure both files are checked/enabled in the list
5. Click **Apply** → **OK**

> **Tip:** The `envVariables` symlink always points to the latest generated file, so you only need to add it once — just re-run `ephemeralEnvVars.py` when you switch envs and update `configs.py`.

---

## Quick Reference

| Task | Command |
|------|---------|
| Switch env | Edit `ENV_NAME` in `configs.py` |
| Regenerate env vars | `cd env-setup && python3 ephemeralEnvVars.py` |
| Refresh AWS creds | Copy from AWS console → run `makeenv` |
| Run tests | Use the IntelliJ run config created above |
