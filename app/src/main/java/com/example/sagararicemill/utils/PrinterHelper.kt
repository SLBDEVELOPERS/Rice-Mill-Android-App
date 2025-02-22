package com.example.sagararicemill.utils

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
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

    companion object {
        private const val SHARED_PREFS_NAME = "PrinterPrefs"
        private const val KEY_PRINTER_MAC = "printer_mac"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Stores the selected printer's MAC address in SharedPreferences.
     *
     * @param macAddress The MAC address of the printer.
     */
    fun savePrinterMacAddress(macAddress: String) {
        sharedPreferences.edit().putString(KEY_PRINTER_MAC, macAddress).apply()
    }

    /**
     * Retrieves the stored printer's MAC address from SharedPreferences.
     *
     * @return The MAC address of the printer, or null if not found.
     */
    fun getPrinterMacAddress(): String? {
        return sharedPreferences.getString(KEY_PRINTER_MAC, null)
    }

    /**
     * Prints a bill using the stored printer's MAC address.
     *
     * @param shop The shop to which the bill is issued.
     * @param orders The list of orders included in the bill.
     * @param bill The bill details.
     */
    fun printBill(shop: Shop, orders: List<Order>, bill: Bill) {
        val macAddress = getPrinterMacAddress()
        if (macAddress.isNullOrEmpty()) {
            Toast.makeText(context, "No printer selected. Please pair a printer first.", Toast.LENGTH_SHORT).show()
            return
        }

        val connection = BluetoothPrintersConnections.selectFirstPaired()?.let {
            if (it.device.address == macAddress) it else null
        }

        if (connection == null) {
            Toast.makeText(context, "Printer not found or not paired.", Toast.LENGTH_SHORT).show()
            return
        }

        val printer = EscPosPrinter(
            connection,
            203, // DPI
            48f,  // Paper width in mm
            32   // Characters per line
        )

        try {
            val billContent = generateBillContent(shop, orders, bill)
            printer.printFormattedText(billContent)
            Toast.makeText(context, "Bill sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            Toast.makeText(context, "Printer connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: EscPosEncodingException) {
            e.printStackTrace()
            Toast.makeText(context, "Error encoding text: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates the bill content in a printable format.
     *
     * @param shop The shop to which the bill is issued.
     * @param orders The list of orders included in the bill.
     * @param bill The bill details.
     * @return The formatted bill content as a String.
     */
    private fun generateBillContent(shop: Shop, orders: List<Order>, bill: Bill): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val billDate = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

        val sb = StringBuilder()
        sb.append("<C><B>SAGARA RICE MILL</B></C>\n")
        sb.append("================================\n")
        sb.append("<C>Date: $billDate</C>\n")
        sb.append("<C>Bill ID: ${bill.id}</C>\n")
        sb.append("--------------------------------\n")
        sb.append("<L>Shop Name: ${shop.name}</L>\n")
        sb.append("<L>Address: ${shop.address}</L>\n")
        sb.append("<L>Contact: ${shop.contact}</L>\n")
        sb.append("================================\n")
        sb.append("<B>ITEM               QTY   PRICE   TOTAL</B>\n")
        sb.append("================================\n")
        for (order in orders) {
            sb.append(
                "<L>${order.size.padEnd(18)} ${order.quantity.toString().padEnd(4)} " +
                        "Rs ${order.price.toString().padEnd(5)} Rs ${order.totalPrice}</L>\n"
            )
        }
        sb.append("================================\n")
        sb.append("<L>Subtotal: Rs ${"%.2f".format(bill.amount)}</L>\n")
        sb.append("<L>Discount: Rs ${"%.2f".format(50.0)}</L>\n") // Example discount
        sb.append("<L>Tax (5%): Rs ${"%.2f".format(bill.amount * 0.05)}</L>\n")
        sb.append("<L>Transport Fee: Rs ${"%.2f".format(100.0)}</L>\n") // Example transport fee
        sb.append("--------------------------------\n")
        sb.append("<B>Grand Total: Rs ${"%.2f".format(bill.amount + (bill.amount * 0.05) + 100.0 - 50.0)}</B>\n")
        sb.append("================================\n")
        sb.append("<C>Payment Method: ${bill.paymentMethod}</C>\n")
        sb.append("--------------------------------\n")
        sb.append("<C>Thank you for your business!</C>\n")
        sb.append("================================\n")

        return sb.toString()
    }

    /**
     * Pairs a Bluetooth printer and stores its MAC address.
     */
    fun pairPrinter() {
        val pairedPrinters = BluetoothPrintersConnections.selectFirstPaired()
        if (pairedPrinters == null) {
            Toast.makeText(context, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }

        savePrinterMacAddress(pairedPrinters.device.address)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        Toast.makeText(context, "Printer paired: ${pairedPrinters.device.name}", Toast.LENGTH_SHORT).show()
    }
}