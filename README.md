# protod

![license](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
![version](https://img.shields.io/badge/Version-1.1.1-green.svg)

Protobuf Decompiler

## Usage

protod is a command-line tool, and does not have a GUI.

```
Usage: protod options_list
Arguments: 
    input -> Input file { String }
    output -> Output path { String }
Options: 
    --exclude [google/protobuf/compiler/plugin.proto, google/protobuf/any.proto, google/protobuf/api.proto, google/protobuf/descriptor.proto, google/protobuf/duration.proto, google/protobuf/empty.proto, google/protobuf/field_mask.proto, google/protobuf/source_context.proto, google/protobuf/struct.proto, google/protobuf/timestamp.proto, google/protobuf/type.proto, google/protobuf/wrappers.proto] -> Exclude files { String }
    --help, -h -> Usage info 
```
