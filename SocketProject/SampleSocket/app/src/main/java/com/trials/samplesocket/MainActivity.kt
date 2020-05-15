package com.trials.samplesocket

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.trials.samplesocket.network.ClientSocket
import com.trials.samplesocket.network.MyServerSocket
import kotlinx.android.synthetic.main.activity_main.*
import android.arch.lifecycle.ViewModelProviders
import android.util.Log
import com.trials.samplesocket.network.SearchRemoteDevices
import com.trials.samplesocket.room.entity.DeviceEntity
import com.trials.samplesocket.viewmodel.DeviceViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var clientSocket: ClientSocket
    private lateinit var serverSocket: MyServerSocket
    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var searchRemoteDevices: SearchRemoteDevices
    private val requestPermissions = listOf(Manifest.permission.ACCESS_NETWORK_STATE)
    private var requestCount = 0

    private val superVisorJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + superVisorJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set adapter
        val viewManager = LinearLayoutManager(this)
        val deviceAdapter = DevicesAdapter(this)
        list_devices.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            // specify an viewAdapter (see also next example)
            adapter = deviceAdapter
            // use a linear layout manager
            layoutManager = viewManager
        }

        deviceViewModel = ViewModelProviders.of(this).get(DeviceViewModel::class.java)
        deviceViewModel.allDevices.observe(this,
            Observer<List<DeviceEntity>> { t ->
                deviceAdapter.setDevices(t)
            })

    }

    private var parent: Job? = null
    private val target = 0

    private suspend fun func2(list: List<Int>): Boolean {
        var res = true
        for (i in list) {
            if (!check(i, target)) {
                res = false
                break
            }
        }
        return res
    }

    private suspend fun check(index: Int, target: Int): Boolean {
        delay(15L)
        return (index != target)
    }

    private suspend inline fun List<Int>.process01(scope: CoroutineScope) =
        suspendCoroutine<Boolean> { continuation ->
            scope.launch {
                withContext(Dispatchers.Default) {
                    for (i in iterator()) {
                        CoroutineScope(this@withContext.coroutineContext).launch {
                            if (!check(i, target)) {
                                scope.coroutineContext.cancelChildren()
                                continuation.resume(false)
                            }
                        }
                    }
                }
                continuation.resume(true)
            }
        }

    private suspend inline fun List<Int>.process02(parent: CoroutineScope): Boolean =
        coroutineScope {
            val separator = 2
            val size = size
            val list1 = subList(0, size / separator)
            val list2 = subList(size / separator, size)
            val res1 = parent.async { func2(list1) }
            val res2 = parent.async { func2(list2) }
            val res = res1.await() && res2.await()
            return@coroutineScope res
        }

    override fun onResume() {
        super.onResume()
/*        var total1 = 0L
        var total2 = 0L
        val max = 100
        val size = 300
        launch {
            repeat(max) {
                val randomNum = if (it % 2 == 0) Random.nextInt(0, size) else size
                val list = List(size) { index ->
                    if (index == randomNum) {
                        0
                    } else {
                        1
                    }
                }
                Log.e(
                    MainActivity::class.java.simpleName,
                    "($it) list size -> ${list.size} random num -> $randomNum"
                )
                val time = measureTimeMillis {
                    val res = list.process01(CoroutineScope(coroutineContext))
                    Log.d(
                        MainActivity::class.java.simpleName, "func1(): res -> $res"
                    )
                }
                total1 += time
                Log.d(
                    MainActivity::class.java.simpleName,
                    "func1(): processing time -> $time, total -> $total1 ave -> ${total1 / (it + 1)}"
                )
                val time3 = measureTimeMillis {
                    val res = func2(list)
                    Log.d(
                        MainActivity::class.java.simpleName,
                        "func3(): res -> $res"
                    )
                }
                total2 += time3
                Log.d(
                    MainActivity::class.java.simpleName,
                    "func3(): processing time -> $time3, total -> $total2 ave -> ${total2 / (it + 1)}"
                )
                delay(100L)
            }
        }
        val port = 7070
        if (!::serverSocket.isInitialized) {
            serverSocket = MyServerSocket(port)
        }
        serverSocket.start()
        if (!::clientSocket.isInitialized) {
            clientSocket = ClientSocket("192.168.3.3", port)
        }
        clientSocket.start()
        if (!::searchRemoteDevices.isInitialized) {
            searchRemoteDevices = SearchRemoteDevices(application, port)
        }
        searchRemoteDevices.setSearchInterface(object :
            SearchRemoteDevices.SearchDevicesInterface() {
            override fun onComplete(devices: List<String>) {
                Log.d(TAG, "onComplete() devices -> $devices")
            }

            override fun onDetected(ip: String) {
                Log.d(TAG, "onComplete() devices -> $ip")
                val device = DeviceEntity()
                device.deviceName = ip
                deviceViewModel.insert(device)
            }

            override fun onNothing() {
                Log.d(TAG, "onNothing()")
            }
        })
//        searchRemoteDevices.searchDevices()*/
        val s = Sample()
    }

    override fun onPause() {
        super.onPause()
        if (::serverSocket.isInitialized) {
            serverSocket.close()
        }
        if (::clientSocket.isInitialized) {
            clientSocket.close()
        }
        if (::searchRemoteDevices.isInitialized) {
            searchRemoteDevices.cancelSearch()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            if (!requestRequiredPermissions()) {

            }
        }
    }

    private fun requestRequiredPermissions(): Boolean {
        if (requestCount < 3) {
            val requiredPermissions = ArrayList<String>()
            for (p in requestPermissions) {
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(p)
                }
            }
            if (requiredPermissions.isNotEmpty()) {
                requestCount++
                requestPermissions(requiredPermissions.toTypedArray(), 100)
                return true
            }
        }
        return false
    }
    class Sample {

        private var isChanged by Delegates.observable<Boolean?>(null){_, old, new ->
            Log.e(Sample::class.java.simpleName,"$old -> $new")
            if (old == false && new == true) {
                Log.e(Sample::class.java.simpleName,"isChanged")
            }
        }

        init {
            GlobalScope.launch {
                while (isActive) {
                    isChanged = true
                    delay(2_000L)
                }
            }
        }

        private fun onDestroy(function: () -> Unit) {
            function.invoke()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private fun func() {
            Log.e(TAG,"func invoked")
        }
    }
}
