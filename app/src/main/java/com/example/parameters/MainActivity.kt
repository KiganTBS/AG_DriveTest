package com.example.parameters

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MicrophoneInfo.Coordinate3F
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.*


class MainActivity : AppCompatActivity() {
    private val mHandler = Handler()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            val timer = Timer()
            location()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.post {
                        tvInfo.text = checkNetworkCapabilities()
                    }
                }
            }, 0, 5000)
        }
    }

    @SuppressLint("Range")
    private fun checkNetworkCapabilities(): String {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // Check if the network is roaming
        val isNetworkRoaming =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) == false

        // Check if the device is connected to a VPN
        val isNetworkVpn =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) == false

        // Check if the device is 5G capable
        val isDevice5GCapable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            networkCapabilities?.hasCapability(NET_CAPABILITY_NOT_METERED) == true ||
                    networkCapabilities?.hasCapability(NET_CAPABILITY_TEMPORARILY_NOT_METERED) == true
        } else {
            false
        }

        // Device connected to internet
        val isConnectedToInternet =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false

        // Device connected to wifi
        val isConnectedToWifi =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

        // Device is international roaming
        val isInternationalRoaming =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) == false
                    && networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false

        val dnsServers =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)?.dnsServers?.joinToString(
                ", "
            ) ?: ""

        val apnState = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS) ?: false

        val uid = Process.myUid()
        val rxBytesBefore = TrafficStats.getUidRxBytes(uid)
        val txBytesBefore = TrafficStats.getUidTxBytes(uid)
        val startTime = System.currentTimeMillis()

        Thread.sleep(1000)

        val rxBytesAfter = TrafficStats.getUidRxBytes(uid)
        val txBytesAfter = TrafficStats.getUidTxBytes(uid)
        val endTime = System.currentTimeMillis()

        val rxSpeedKbps = ((rxBytesAfter - rxBytesBefore) * 8.0 / 1000) / ((endTime - startTime) / 1000.0)
        val txSpeedKbps = ((txBytesAfter - txBytesBefore) * 8.0 / 1000) / ((endTime - startTime) / 1000.0)

        Log.d("AndroidRuntime NetworkSpeed", "Rx Speed: ${rxSpeedKbps} kbps")
        Log.d("AndroidRuntime NetworkSpeed", "Tx Speed: ${txSpeedKbps} kbps")


        // To get current time Fri Nov 08 11:37:53 WAT 2019
        val currentTime = Calendar.getInstance().time

        // To get Time zone [id="Africa/Luanda",offset=3600000,dstSavings=0,useDaylight=false,transitions=3,lastRule=null]
        val timeZone = Calendar.getInstance().timeZone

        // Device fingerprint info
        val fingerPrint = deviceID()

        // Device name of the underlying board
        val deviceBoard = Build.BOARD

        // Device Brand name
        val deviceBrand = Build.BRAND

        // Name of the industrial design
        val deviceIndustrialName = Build.DEVICE

        // A build ID string meant for displaying to the user
        val deviceUserBuildId = Build.DISPLAY

        // Device chipset name
        val deviceChipset = Build.HARDWARE

        // Device name overall
        val deviceOverall = Build.PRODUCT

        // Device Radio version
        val deviceRadioVer = Build.getRadioVersion()

        // Store download name
        val appStoreName = appStore()

        // Device information about the device's sensors
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val sensorNames = sensorList.map { it.name }

        // Device screen size
        val screenSize =
            "${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}"

        // Device Os version
        val osVersion = Build.VERSION.RELEASE

        // Device SDK version
        val sdkVersion = Build.VERSION.SDK_INT

        // Device cpu arch
        val cpuArch = System.getProperty("os.arch")

        return "Network Roaming: $isNetworkRoaming\n-----------------------------------\n" +
                "International roaming: $isInternationalRoaming\n-----------------------------------\n" +
                "VPN: $isNetworkVpn\n-----------------------------------\n" +
                "APN State: $apnState\n-----------------------------------\n" +
                "Connected to internet: $isConnectedToInternet\n-----------------------------------\n" +
                "Connected to wifi: $isConnectedToWifi\n-----------------------------------\n" +
                "${networkTypeConnection(this)}\n-----------------------------------\n" +
                "Network class type: ${getNetworkClass(this)}\n-----------------------------------\n" +
                "5G Capable: $isDevice5GCapable\n-----------------------------------\n" +
                "Down stream Bandwidth Kbps: ${getDownstreamBandwidthKbps()}\n-----------------------------------\n" +
                "Up stream Bandwidth Kbps: ${getUpstreamBandwidthKbps()}\n-----------------------------------\n" +
                "DNS server: $dnsServers\n-----------------------------------\n" +
                "Current Time: $currentTime\n-----------------------------------\n" +
                "Time Zone: $timeZone\n-----------------------------------\n" +
                "Device Finger Print: $fingerPrint\n-----------------------------------\n" +
                "Device Board: $deviceBoard \n-----------------------------------\n" +
                "Device Brand: $deviceBrand \n-----------------------------------\n" +
                "Name of the industrial design: $deviceIndustrialName\n-----------------------------------\n" +
                "A build ID string meant for displaying to the user: $deviceUserBuildId \n-----------------------------------\n" +
                "Device chipset name: $deviceChipset \n-----------------------------------\n" +
                "Device name overall: $deviceOverall\n-----------------------------------\n" +
                "Device Radio version $deviceRadioVer \n-----------------------------------\n" +
                "Sensors: $sensorNames \n-----------------------------------\n" +
                "Screen size: $screenSize \n-----------------------------------\n" +
                "OS version: $osVersion  \n-----------------------------------\n" +
                "SDK version: $sdkVersion \n-----------------------------------\n" +
                "CPU architecture: $cpuArch \n-----------------------------------\n" +
                "Download from: $appStoreName \n-----------------------------------\n" +
                "Battery info:\n${batteryInfo()} \n-----------------------------------\n" +
                "${ramInfo()} \n-----------------------------------\n" +
                "${storageInfo()} \n-----------------------------------\n" +
                "${simCardInfo()} \n-----------------------------------\n" +
                "${internetInfo()} \n-----------------------------------\n" +
                "${getPingStatistics()} \n-----------------------------------\n" +
                "${permissionInfo()} \n-----------------------------------\n" +
                "${location()} \n-----------------------------------\n"

    }

    private fun getNetworkClass(context: Context): String? {
        val mTelephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        var networkType: Int?
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        }
        networkType = mTelephonyManager.networkType

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    private fun simCardInfo() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simState = telephonyManager.simState
        val signalStrength = telephonyManager.signalStrength
        val rsrpDbm = -140 + 2 * signalStrength?.cellSignalStrengths?.get(0)?.asuLevel!!

        val isConcurrentVoiceDataSupported = telephonyManager.isConcurrentVoiceAndDataSupported
        val isDeviceDataRoaming = telephonyManager.isDataRoamingEnabled

        //   val isWorldPhone = telephonyManager.isWorldPhone

        var activeModemCount: Int? = null
        var supportedModem: Int? = null
        var isDeviceDataConnectionAllowed: Boolean? = null
        var isDeviceDataCapable: Boolean? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                isDeviceDataConnectionAllowed = telephonyManager.isDataConnectionAllowed
            }
            isDeviceDataCapable = telephonyManager.isDataCapable
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activeModemCount = telephonyManager.activeModemCount
            supportedModem = telephonyManager.supportedModemCount
        }

        //RSRQDB
        val cellInfo = telephonyManager.allCellInfo.firstOrNull { it is CellInfoNr } as CellInfoNr?
        val ss = cellInfo?.cellSignalStrength as? CellSignalStrengthNr
        val rsrqDb = (ss?.ssRsrq?.div(4.0)?.minus(20.0)) ?: ((ss?.csiRsrq?.div(2.0)?.minus(23.5)) ?: 0.0)
        val ssRsrq = ss?.ssRsrq
        val csiRsrq = ss?.csiRsrq
        val ssRsrp = ss?.ssRsrp
        val csiRsrp = ss?.csiRsrp
        val csiSinr = ss?.csiSinr
        val ssSinr = ss?.ssSinr
        val rssiDbm = ss?.dbm

        Log.d("KiganTBS2222","$ssRsrp / $csiRsrp")

         if (simState == TelephonyManager.SIM_STATE_ABSENT) {
            "No SIM card detected"
        } else {
            val simOperatorName = telephonyManager.simOperatorName
            val simOperator = telephonyManager.simOperator
            val allocationCode = telephonyManager.typeAllocationCode
            val manufacturerCode = telephonyManager.manufacturerCode
            val simState = telephonyManager.simState

            val mcc = simOperator.substring(0, 3)
            val mnc = simOperator.substring(3)
            val simCountryIso = telephonyManager.simCountryIso
            val simCount = telephonyManager.phoneCount
            val isMultiSimSupport = simCount > 1

        }
    }

    private fun permissionInfo(): String {
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasNetworkStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
        val hasWifiStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        val hasComponentEnabledStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CHANGE_COMPONENT_ENABLED_STATE) == PackageManager.PERMISSION_GRANTED
        val hasWifiMulticastPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CHANGE_WIFI_MULTICAST_STATE) == PackageManager.PERMISSION_GRANTED
        val hasBasicPhoneStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_BASIC_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasModifyPhoneStatePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.MODIFY_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasBackgroundLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasMediaLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasHardwareLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.LOCATION_HARDWARE) == PackageManager.PERMISSION_GRANTED
        val hasInternetPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED

        return "Has Phone State Permission: $hasPhoneStatePermission \n-----------------------------------\n" +
                "Has Network State Permission: $hasNetworkStatePermission \n-----------------------------------\n" +
                "Has Wifi State Permission: $hasWifiStatePermission \n-----------------------------------\n" +
                "Has Component Enabled State Permission: $hasComponentEnabledStatePermission \n-----------------------------------\n" +
                "Has Wifi Multicast Permission: $hasWifiMulticastPermission \n-----------------------------------\n" +
                "Has Basic Phone State Permission: $hasBasicPhoneStatePermission \n-----------------------------------\n" +
                "Has Modify Phone State Permission: $hasModifyPhoneStatePermission \n-----------------------------------\n" +
                "Has Coarse Location Permission: $hasCoarseLocationPermission \n-----------------------------------\n" +
                "Has Fine Location Permission: $hasFineLocationPermission \n-----------------------------------\n" +
                "Has Background Location Permission: $hasBackgroundLocationPermission \n-----------------------------------\n" +
                "Has Media Location Permission: $hasMediaLocationPermission \n-----------------------------------\n" +
                "Has Hardware Location Permission: $hasHardwareLocationPermission \n-----------------------------------\n" +
                "Has Internet Permission: $hasInternetPermission \n-----------------------------------\n"

    }

    private fun storageInfo(): String {
        val externalStorageState = Environment.getExternalStorageState()
        val isExternalStorageReadable =
            externalStorageState == Environment.MEDIA_MOUNTED || externalStorageState == Environment.MEDIA_MOUNTED_READ_ONLY
        val internalStorageDirectory = Environment.getDataDirectory()
        val totalInternalStorage = internalStorageDirectory.totalSpace / (1024 * 1024)
        val availableInternalStorage = internalStorageDirectory.usableSpace / (1024 * 1024)

        return "Is external storage readable: $isExternalStorageReadable\n-----------------------------------\n" +
                "Total internal storage: $totalInternalStorage MB\n-----------------------------------\n" +
                "Available internal storage: $availableInternalStorage MB\n-----------------------------------\n"
    }

    private fun ramInfo(): String {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRam = memoryInfo.totalMem / (1024 * 1024)
        val availableRam = memoryInfo.availMem / (1024 * 1024)

        return "Total RAM: $totalRam MB \n-----------------------------------\n" +
                "Available RAM: $availableRam MB \n-----------------------------------\n"

    }

    private fun internetInfo(): String? {
        //ipV6
        val networkInterface = NetworkInterface.getByName("wlan0")
        val ipv6Address: String? = networkInterface?.inetAddresses
            ?.asSequence()
            ?.filter { it is Inet6Address && !it.isLinkLocalAddress }
            ?.map { it.hostAddress }
            ?.firstOrNull()

        //ipV4
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = BigInteger.valueOf(wifiManager.connectionInfo.ipAddress.toLong()).let {
            String.format(
                "%d.%d.%d.%d",
                it.shiftRight(24).and(BigInteger.valueOf(0xff)).toLong(),
                it.shiftRight(16).and(BigInteger.valueOf(0xff)).toLong(),
                it.shiftRight(8).and(BigInteger.valueOf(0xff)).toLong(),
                it.and(BigInteger.valueOf(0xff)).toLong()
            )
        }

        return "ipv6Address: $ipv6Address\n ipV4: $ipAddress"
    }

    fun networkTypeConnection(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Network Type Connection: mobile"
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "Network Type Connection: WI-FI"
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "Network Type Connection: transport internet"
                }
            }
        }
        return "Network Type Connection: Undefined"
    }

    fun getPingStatistics(): String {
        val hostname = "example.com"

        var hopCount = 0
//        Thread {
//            val address = InetAddress.getByName(hostname)
//            val traceroute = Runtime.getRuntime().exec("traceroute -m 30 $hostname")
//            val reader = BufferedReader(InputStreamReader(traceroute.inputStream))
//            var line: String? = reader.readLine()
//
//            while (line != null) {
//                hopCount++
//                line = reader.readLine()
//            }
//        }.start()


        return "Number of traceroute hops to $hostname: $hopCount"
    }

    private fun deviceID(): String? {
        return try {
            val query: Cursor =
                this.contentResolver.query(sUri, null, null, arrayOf("android_id"), null)
                    ?: return Build.VERSION.BASE_OS
            if (!query.moveToFirst() || query.columnCount < 2) {
                query.close()
                return Build.VERSION.BASE_OS
            }
            val toHexString = java.lang.Long.toHexString(query.getString(1).toLong())
            query.close()
            toHexString.uppercase(Locale.getDefault()).trim { it <= ' ' }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Build.VERSION.BASE_OS
        } catch (e2: java.lang.Exception) {
            e2.printStackTrace()
            Build.VERSION.BASE_OS
        }
    }

    private fun appStore(): String {
        return when (applicationContext.packageManager.getInstallerPackageName(applicationContext.packageName)) {
            "com.android.vending" -> "Google Play Store"
            "com.amazon.venezia" -> "Amazon Appstore"
            "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
            else -> {
                "undefine"
            }
        }
    }

    private fun batteryInfo(): String {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

        return "Battery level: $batteryLevel\n" + when (batteryStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Battery status: Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Battery status: Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Battery status: Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Battery status: Not charging"
            else -> "Battery status: Unknown"
        }
    }

    private fun getDownstreamBandwidthKbps(): Int? {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        return networkCapabilities.linkDownstreamBandwidthKbps
    }

    private fun getUpstreamBandwidthKbps(): Int? {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        return networkCapabilities.linkUpstreamBandwidthKbps
    }

    private fun location(): String?{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var startLocation: Location? = null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Define a variable to hold the initial location


            // Define a LocationListener
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Called when the location has changed
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("AndroidRuntime TAG", "Latitude: $latitude, Longitude: $longitude")
                }

                override fun onProviderEnabled(provider: String) {
                    // Called when the user enables the location provider
                }

                override fun onProviderDisabled(provider: String) {
                    // Called when the user disables the location provider
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // Called when the status of the location provider changes
                }
            }

            // Get the last known location and save it to startLocation
            startLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            // Request location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

            // If startLocation is not null, get the starting latitude and longitude
            if (startLocation != null) {
                val startLatitude = startLocation.latitude
                val startLongitude = startLocation.longitude
                Log.d("AndroidRuntime TAG", "Start Latitude: $startLatitude, Start Longitude: $startLongitude")
            }
        }
        return startLocation.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private val sUri: Uri = Uri.parse("content://com.google.android.gsf.gservices")
    }
}

/**
Android:
1. information about second sim card only with permission READ_PHONE_STATE
2. ipV4 only with uses-permission android.permission.ACCESS_WIFI_STATE
3. ipV6 only with uses-permission android.permission.INTERNET
4. All about latency - need a server to know this
5. Is world phone - is need Permission for only granted system apps
6. isDeviceDataConnectionAllowed - need READ_PHONE_STATE and SDK>31
7. isDeviceServiceState - need permission ACCESS_FINE_LOCATION
8. Thermal - need to include NDK
9. APN - is need Permission for only granted system apps
10. Network class type - is need Manifest.permission.READ_PHONE_STATE
 */