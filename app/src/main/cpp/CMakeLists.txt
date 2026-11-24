cmake_minimum_required(VERSION 3.22.1)
project(luna_receiver LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
add_library(usb1.0 SHARED
 libusb/libusb/core.c libusb/libusb/descriptor.c libusb/libusb/hotplug.c
 libusb/libusb/io.c libusb/libusb/sync.c libusb/libusb/strerror.c
 libusb/libusb/os/linux_usbfs.c libusb/libusb/os/events_posix.c
 libusb/libusb/os/threads_posix.c libusb/libusb/os/linux_netlink.c)
target_include_directories(usb1.0 PRIVATE libusb/android libusb/libusb libusb/libusb/os)
target_compile_options(usb1.0 PRIVATE -fvisibility=hidden -pthread)
target_link_libraries(usb1.0 log)
add_library(luna-receiver SHARED receiver_jni.cpp
 xow/dongle/usb.cpp xow/dongle/mt76.cpp xow/dongle/dongle.cpp
 xow/controller/controller.cpp xow/controller/gip.cpp xow/utils/crypto.cpp xow/utils/log.cpp)
target_include_directories(luna-receiver PRIVATE xow libusb)
target_compile_options(luna-receiver PRIVATE -Wall -Wextra)
target_link_libraries(luna-receiver usb1.0 log)
