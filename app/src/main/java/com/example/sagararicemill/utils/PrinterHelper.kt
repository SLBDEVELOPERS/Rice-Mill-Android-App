package com.example.sagararicemill.utils

import android.content.Context
import android.widget.Toast
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.Shop
import java.text.SimpleDateFormat
import java.util.*


class PrinterHelper(private val context: Context) {

    /**
     * Prints a bill referencing a single order.
     *
     * @param shop The shop to which the bill is issued.
     * @param order The order details.
     * @param bill The bill details.
     */
    fun printBill(shop: Shop, order: Order, bill: Bill) {
        val connection = BluetoothPrintersConnections.selectFirstPaired()

        if (connection == null) {
            Toast.makeText(context, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }

        val printer = EscPosPrinter(
            connection,
            203, // DPI
            48f,  // Paper width in mm
            32   // Characters per line
        )

        try {
            val sb = StringBuilder()
            sb.append("<C><B>Rice Mill Management</B></C>\n")
            sb.append("\n")
            sb.append("<C>Bill ID: ${bill.id}</C>\n")
            sb.append("<L>Shop: ${shop.name}</L>\n")
            sb.append("<L>Address: ${shop.address}</L>\n")
            sb.append("<L>Contact: ${shop.contact}</L>\n")
            sb.append("\n")
            sb.append("<B>Order Details:</B>\n")
            sb.append("Order ID: ${order.id}\n")
            sb.append("Rice Bag: ${order.riceBagId}\n")
            sb.append("Quantity: ${order.quantity}\n")
            sb.append("Total Price: \$${String.format("%.2f", order.totalPrice)}\n")
            sb.append("Delivery Status: ${order.deliveryStatus}\n")
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sb.append("Order Date: ${sdf.format(order.orderDate?.toDate())}\n\n")
            sb.append("<B>Total Amount: \$${String.format("%.2f", bill.amount)}</B>\n")
            sb.append("\n")
            sb.append("<C>Thank you!</C>\n")

            printer.printFormattedText(sb.toString())
            Toast.makeText(context, "Bill sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            Toast.makeText(context, "Printer connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosEncodingException) {
            e.printStackTrace()
            Toast.makeText(context, "Error encoding text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Prints a bill referencing multiple orders.
     *
     * @param shop The shop to which the bill is issued.
     * @param orders The list of orders included in the bill.
     * @param bill The bill details.
     */
    fun printBillMultiOrder(shop: Shop, orders: List<Order>, bill: Bill) {
        val connection = BluetoothPrintersConnections.selectFirstPaired()

        if (connection == null) {
            Toast.makeText(context, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }

        val printer = EscPosPrinter(
            connection,
            203, // DPI
            48f,  // Paper width in mm
            32   // Characters per line
        )

        try {
            val sb = StringBuilder()
            sb.append("<C><B>Rice Mill Management</B></C>\n")
            sb.append("\n")
            sb.append("<C>Bill ID: ${bill.id}</C>\n")
            sb.append("<L>Shop: ${shop.name}</L>\n")
            sb.append("<L>Address: ${shop.address}</L>\n")
            sb.append("<L>Contact: ${shop.contact}</L>\n")
            sb.append("\n")
            sb.append("<B>Order Details:</B>\n")

            for (order in orders) {
                sb.append("Order ID: ${order.id}\n")
                sb.append("Rice Bag: ${order.riceBagId}\n")
                sb.append("Quantity: ${order.quantity}\n")
                sb.append("Total Price: \$${String.format("%.2f", order.totalPrice)}\n")
                sb.append("Delivery Status: ${order.deliveryStatus}\n")
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sb.append("Order Date: ${sdf.format(order.orderDate?.toDate())}\n\n")
            }

            sb.append("<B>Total Amount: \$${String.format("%.2f", bill.amount)}</B>\n")
            sb.append("\n")
            sb.append("<C>Thank you!</C>\n")

            printer.printFormattedText(sb.toString())
            Toast.makeText(context, "Bill sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            Toast.makeText(context, "Printer connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosEncodingException) {
            e.printStackTrace()
            Toast.makeText(context, "Error encoding text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Prints a textual report.
     *
     * @param reportContent The content of the report to be printed.
     */
    fun printReport(reportContent: String) {
        val connection = BluetoothPrintersConnections.selectFirstPaired()

        if (connection == null) {
            Toast.makeText(context, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }

        val printer = EscPosPrinter(
            connection,
            203, // DPI
            48f,  // Paper width in mm
            32   // Characters per line
        )

        try {
            printer.printFormattedText(reportContent)
            Toast.makeText(context, "Report sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            Toast.makeText(context, "Printer connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosEncodingException) {
            e.printStackTrace()
            Toast.makeText(context, "Error encoding text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Selects a Bluetooth printer by its MAC address.
     *
     * @param macAddress The MAC address of the desired printer.
     * @return The DeviceConnection if found and paired, else null.
     */
//    fun selectPrinterByMAC(macAddress: String): EscPosPrinter? {
//        val device = BluetoothPrintersConnections.getBluetoothPrinters().find { it.address == macAddress }
//        return if (device != null) {
//            EscPosPrinter(
//                BluetoothPrintersConnections.bluetooth(device),
//                203, // DPI
//                48f,  // Paper width in mm
//                32   // Characters per line
//            )
//        } else {
//            Toast.makeText(context, "Printer with MAC $macAddress not found", Toast.LENGTH_SHORT).show()
//            null
//        }
//    }
}


