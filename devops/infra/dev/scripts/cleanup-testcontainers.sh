#!/bin/bash

echo "List running testcontainers:"
podman ps -a --filter "label=cosmolab-testcontainer=true"

echo "Cleaning up Testcontainers resources..."
podman ps -a --filter "label=cosmolab-testcontainer=true" --format "{{.ID}}" | xargs -r podman rm -f
echo "Cleanup complete."
