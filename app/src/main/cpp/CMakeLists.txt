cmake_minimum_required(VERSION 3.22.1)

project("sponsorflow_native")

# Compilación de la librería puente C++ para Android.
# Más adelante aquí vincularemos el código fuente de llama.cpp y whisper.cpp
add_library(sponsorflow_native SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(sponsorflow_native ${log-lib})
