cmake_minimum_required(VERSION 3.22.1)
project("taskflow")

add_library(taskflow SHARED native-lib.cpp)

target_link_libraries(taskflow
        android
        log)
