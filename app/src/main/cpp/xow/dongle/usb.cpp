/*
 * Copyright (C) 2019 Medusalix
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */


#include <libusb/libusb.h>
#include "usb.h"
#include "../utils/log.h"

// Timeouts in milliseconds
#define USB_TIMEOUT_READ 1000
#define USB_TIMEOUT_WRITE 1000
// Local adaptation: one wrapped Android file descriptor, checked construction and cleanup.
UsbDevice::UsbDevice(int fd) : ctx(nullptr), handle(nullptr)
{
    libusb_init_option option{};
    option.option = LIBUSB_OPTION_NO_DEVICE_DISCOVERY;
    int r = libusb_init_context(&ctx, &option, 1);
    if (r < 0) throw std::runtime_error(libusb_error_name(r));
    r = libusb_wrap_sys_device(ctx, (intptr_t)fd, &handle);
    if (r == 0 && handle) r = libusb_claim_interface(handle, 0);
    else if (r == 0) r = LIBUSB_ERROR_NO_DEVICE;
    if (r < 0) {
        if (handle) libusb_close(handle);
        libusb_exit(ctx);
        handle = nullptr; ctx = nullptr;
        throw std::runtime_error(libusb_error_name(r));
    }
}

UsbDevice::~UsbDevice()
{
    if (handle) { libusb_release_interface(handle, 0); libusb_close(handle); }
    if (ctx) libusb_exit(ctx);
}

void UsbDevice::controlTransfer(ControlPacket packet, bool write)
{
    uint8_t direction = write ? LIBUSB_ENDPOINT_OUT : LIBUSB_ENDPOINT_IN;

    // Number of bytes or error code
    int transferred = libusb_control_transfer(
            handle,
            LIBUSB_REQUEST_TYPE_VENDOR | LIBUSB_RECIPIENT_DEVICE | direction,
            packet.request,
            packet.value,
            packet.index,
            packet.data,
            packet.length,
            USB_TIMEOUT_WRITE
    );

    if (transferred != packet.length)
    {
        Log::error(
                "Error in control transfer: %s",
                libusb_error_name(transferred)
        );
    }
}

int UsbDevice::bulkRead(
        uint8_t endpoint,
        FixedBytes<USB_MAX_BULK_TRANSFER_SIZE> &buffer
) {
    int transferred = 0;
    int error = libusb_bulk_transfer(
            handle,
            endpoint | LIBUSB_ENDPOINT_IN,
            buffer.raw(),
            buffer.size(),
            &transferred,
            USB_TIMEOUT_READ
    );

    if (error && error != LIBUSB_ERROR_TIMEOUT)
    {
        Log::error("Error in bulk read: %s", libusb_error_name(error));

        return -1;
    }

    return transferred;
}

bool UsbDevice::bulkWrite(uint8_t endpoint, Bytes &data)
{
    int transferred = 0;
    int error = libusb_bulk_transfer(
            handle,
            endpoint | LIBUSB_ENDPOINT_OUT,
            data.raw(),
            data.size(),
            &transferred,
            USB_TIMEOUT_WRITE
    );

    if (error)
    {
        Log::error("Error in bulk write: %s", libusb_error_name(error));

        return false;
    }

    return true;
}
