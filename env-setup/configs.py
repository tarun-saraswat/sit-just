import os

ENV_NAME = "<ephemeral_env_name>"
FRAMEWORK_PATH = os.path.dirname(os.getcwd())
EPH_PROP = "eph.properties"
GEN_PROPS = "./gradlew clean build -x test && ./gradlew generateProperties --offline --args='"+ ENV_NAME + " swiggyops.de singapore.swig.gy'"

home_dir = os.path.expanduser("~")  # Gets the user's home directory
downloads_path = os.path.join(home_dir, "Downloads")

envVariableKeys = [
                "SHUTTLE_ENV=" + ENV_NAME,
                "SHUTTLE_ENV_LOCATION=in-west.swig.gy",
                "ROCK_TYPE=https",
                "DNS_TYPE=http",
                "GRPC_TYPE=grpc",
                "IS_DYNAMIC_CONFIG=TRUE",
                "GRPC_PEM_FILE_PATH=" + downloads_path,
                "AWS_SESSION_TOKEN=xxxx"
            ]

vmOptionKeys = [
                "KAFKA_TXN_HA_PRIMARY",
                "KAFKA_TXN_PRIMARY",
                "KAFKA_TXN_HA_SECONDARY",
                "KAFKA_TXN_DSP_PRIMARY",
                "KAFKA_BATCH_PRIMARY"
            ]

# Keys from envVariableKeys that also need to be written as lowercase JVM system properties
# so the framework can resolve ${placeholder} syntax in services-configuration.xml
serviceConfigVmOptionKeys = {
    "SHUTTLE_ENV": "shuttle_env",
    "SHUTTLE_ENV_LOCATION": "shuttle_env_location",
    "ROCK_TYPE": "rock_type",
    "DNS_TYPE": "dns_type",
    "GRPC_TYPE": "grpc_type",
}

blackList = {
    "JAVA_TOOL_OPTIONS"
}
