#!/bin/bash

LICENSE=$(cat << 'LICENSE_EOF'
/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

LICENSE_EOF
)

find helpers/schema-registry-standalone examples/schema-registry-demo -name "*.java" | while read -r file; do
    # Strip the existing license we just added
    sed -i '' -e '1,17d' "$file"
    
    # Prepend license with a blank line before the package declaration
    echo "$LICENSE" | cat - "$file" > temp && mv temp "$file"
done
