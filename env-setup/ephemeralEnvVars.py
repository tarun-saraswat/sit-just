import time
import json
import os
import re
import glob
import subprocess
from configs import *

FRAMEWORK_PATH = os.path.dirname(os.getcwd())
print("FRAMEWORK_PATH is :: "+FRAMEWORK_PATH)

def out(command):
    print(f"Executing command: {command}")
    return subprocess.check_output(command, shell=True, universal_newlines=True)

def execCommand(cmd):
    print("exec command: " + cmd)
    result = os.system(cmd)
    if result != 0:
        print(f"Warning: Command returned non-zero exit code: {result}")
    time.sleep(1)
    return result

def getInstanceId():
    command = "coast get status -e " + ENV_NAME + " -s swiggy-test-executor -t swig.gy -r in-west"
    try:
        output = out(command)
        print("The output for command " + command + " is: " + output)
        try:
            start = output.find("{")
            end = len(output) - output[::-1].find("}") + 1
            if start >= 0 and end > start:
                serviceStatus = json.loads(output[start: end])
                if "instancesInfo" in serviceStatus and len(serviceStatus["instancesInfo"]) > 0:
                    instanceId = serviceStatus["instancesInfo"][0]["id"]
                    print("Found instance id: " + instanceId)
                    return instanceId
        except json.JSONDecodeError:
            print("Failed to parse JSON from output")
        match = re.search(r'id:\s*([^\s,]+)', output)
        if match:
            instanceId = match.group(1)
            print(f"Extracted instance ID using regex: {instanceId}")
            return instanceId
        raise Exception("Could not extract instance ID from command output")
    except Exception as e:
        print(f"Error getting instance ID: {str(e)}")
        raise

def getFileName():
    instanceId = getInstanceId()
    command = f"coast debug -f test_rock.sh -i {instanceId} -e {ENV_NAME} -s swiggy-test-executor -r in-west"
    try:
        output = out(command)
        print("The output for command " + command + " is: " + output)
        match = re.search(r's3://rock-debug-output/([a-f0-9-]+)', output)
        if match:
            fileName = match.group(1)
            print("File Name is: " + fileName)
            return fileName.strip()
        match = re.search(r'result be available at\s+([^\s]+)', output)
        if match:
            s3_path = match.group(1)
            fileName = s3_path.split('/')[-1]
            print("File Name is: " + fileName)
            return fileName.strip()
        lines = output.splitlines()
        for line in lines:
            if "s3://" in line:
                parts = line.split("s3://rock-debug-output/")
                if len(parts) > 1:
                    fileName = parts[1].strip()
                    print("File Name is: " + fileName)
                    return fileName
        raise Exception("Could not extract file name from command output")
    except Exception as e:
        print(f"Error getting file name: {str(e)}")
        raise

def getEphProperties():
    try:
        fileName = getFileName()
        print(f"Waiting 5 seconds for file {fileName} to be available...")
        time.sleep(5)
        command = f"coast get report -f {fileName}"
        output = out(command)
        print("The output for command " + command + " is: " + output)
        print("Files in current directory:")
        for f in os.listdir('.'):
            print(f"  {f}")
        possibleFiles = glob.glob(f"{fileName}*") + glob.glob(f"debug-{fileName}*")
        print(f"Found possible files: {possibleFiles}")
        if possibleFiles:
            actualFileName = possibleFiles[0]
            print(f"Using file: {actualFileName}")
            try:
                with open(actualFileName, 'r') as f:
                    content = f.read()
                    if '=' in content:
                        print(f"File {actualFileName} appears to be a properties file")
                    else:
                        print(f"Warning: File {actualFileName} doesn't look like a properties file")
            except Exception as e:
                print(f"Warning: Error reading file {actualFileName}: {str(e)}")
            execCommand(f"mv {actualFileName} {EPH_PROP}")
            execCommand(f"cp {EPH_PROP} {FRAMEWORK_PATH}/src/main/resources/{EPH_PROP}")
            return
        print("Attempting to download file directly from S3...")
        download_cmd = f"aws s3 cp s3://rock-debug-output/{fileName} {EPH_PROP}"
        if execCommand(download_cmd) == 0:
            execCommand(f"cp {EPH_PROP} {FRAMEWORK_PATH}/src/main/resources/{EPH_PROP}")
            print(f"Successfully downloaded file from S3: {fileName}")
            return
        raise Exception(f"Could not find or download the file for {fileName}")
    except Exception as e:
        print(f"Error in getEphProperties: {str(e)}")
        raise

def extractKeys(filePath):
    try:
        if not os.path.exists(filePath):
            raise Exception(f"Properties file not found: {filePath}")

        with open(filePath, "r") as f:
            content = f.read()

        envVarFileName = f"envVariables-{ENV_NAME}"
        vmOptionsFileName = f"vmOptions-{ENV_NAME}"

        db_user_keys = {
            "SHAKTIMAANDB_USER", "DASHPICKERS_USER", "DELIVERYDB_USER",
            "TMSDB_USER", "KMSDB_USER", "SWIGGY_USER"
        }

        db_password_keys = {
            "SHAKTIMAANDB_PASSWORD", "dashpickers", "DELIVERYDB_PASSWORD",
            "tmsdb", "KMSDB_PASSWORD", "shaktimaandb", "deliverydb",
            "SWIGGY_PASSWORD", "swiggy", "TMSDB_PASSWORD", "kmsdb",
            "DASHPICKERS_PASSWORD"
        }

        with open(envVarFileName, "w") as envVarFile, open(vmOptionsFileName, "w") as vmOptionsFile:
            envVars = ""
            vmOptions = ""
            for keyVal in content.split("\n"):
                if "=" in keyVal:
                    key = keyVal.split("=")[0]

                    if key not in blackList:
                        if key in db_user_keys:
                            keyVal = f"{key}=root"
                        elif key in db_password_keys:
                            keyVal = f"{key}=password123"

                        envVars += keyVal + ";\n"
                        if key in vmOptionKeys:
                            vmOptions += "-D" + keyVal + "\n"

            for envVar in envVariableKeys:
                envVars += envVar + ";\n"
                # Also write as lowercase JVM system property for framework placeholder resolution
                key = envVar.split("=")[0]
                if key in serviceConfigVmOptionKeys:
                    val = envVar.split("=", 1)[1]
                    vmOptions += f"-D{serviceConfigVmOptionKeys[key]}={val}\n"

            envVars += "AWS_PROFILE=157529275398_AWSProductEngineering;\n"
            vmOptions += "-D" + "environment=" + ENV_NAME + "\n"
            envVarFile.write(envVars)
            vmOptionsFile.write(vmOptions)

        print(f"Created initial {envVarFileName} and {vmOptionsFileName} files with default DB credentials.")

        print(f"\n--- Starting post-processing to clean {envVarFileName} ---")

        keys_to_force_remove = {
            "AWS_ACCESS_KEY_ID",
            "AWS_SECRET_ACCESS_KEY",
            "AWS_SESSION_TOKEN"
        }

        with open(envVarFileName, "r") as f:
            lines = f.readlines()

        clean_lines = []
        for line in lines:
            line = line.strip()
            if not line:
                continue
            key_part = line.split("=")[0]
            if key_part in keys_to_force_remove:
                print(f"Forcefully removing line for key: {key_part}")
                continue
            clean_lines.append(line)

        with open(envVarFileName, "w") as f:
            f.write("\n".join(clean_lines) + "\n")

        print(f"--- Post-processing complete. {envVarFileName} has been sanitized. ---\n")
        print(f"All DB usernames set to 'root' and all DB passwords set to 'password123'")

        execCommand(f"ln -sf {envVarFileName} envVariables")
        execCommand(f"ln -sf {vmOptionsFileName} vmOptions")
        print("Created symlinks to standard filenames for backward compatibility")

    except Exception as e:
        print(f"Error in extractKeys: {str(e)}")
        raise

def file_exists_with_content(filepath):
    try:
        return os.path.exists(filepath) and os.path.getsize(filepath) > 0
    except:
        return False

def ensureServicesConfig():
    resources_dir = os.path.join(FRAMEWORK_PATH, "src", "main", "resources")
    target = os.path.join(resources_dir, "services-configuration.xml")
    template = os.path.join(resources_dir, "services-config-template-new.xml")
    if os.path.exists(template):
        with open(template, "r") as f:
            content = f.read()
        # Substitute placeholders with actual values so the framework doesn't need -D flags
        substitutions = {
            "${shuttle_env}": ENV_NAME,
            "${shuttle_env_location}": "in-west.swig.gy",
            "${rock_type}": "https",
            "${dns_type}": "http",
            "${grpc_type}": "grpc",
        }
        for placeholder, value in substitutions.items():
            content = content.replace(placeholder, value)
        with open(target, "w") as f:
            f.write(content)
        print(f"Created {target} with env={ENV_NAME} (placeholders resolved)")
    else:
        print(f"Warning: template not found at {template}, skipping services-configuration.xml creation")

def main():
    try:
        ensureServicesConfig()
        getEphProperties()

        if file_exists_with_content(EPH_PROP):
            print(f"Successfully created {EPH_PROP}")
            extractKeys(EPH_PROP)
        else:
            print(f"Error: {EPH_PROP} file is missing or empty")
    except Exception as e:
        print(f"Error in main execution: {str(e)}")
        print("Attempting to use a backup method or previous version...")

if __name__ == "__main__":
    main()
