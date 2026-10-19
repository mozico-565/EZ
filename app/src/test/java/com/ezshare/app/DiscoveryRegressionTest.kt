package com.ezshare.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ClassName
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29],shadows=[DiscoveryRegressionTest.NsdShadow::class])
class DiscoveryRegressionTest {
    @Implements(NsdManager::class) class NsdShadow {
        @Implementation fun __constructor__(context:Context,@ClassName("android.net.nsd.INsdManager") service:Any?){}
        companion object {val searches=mutableListOf<NsdManager.DiscoveryListener>();val resolves=mutableListOf<Pair<NsdServiceInfo,NsdManager.ResolveListener>>()}
        @Implementation fun discoverServices(type:String,protocol:Int,listener:NsdManager.DiscoveryListener){searches+=listener}
        @Implementation fun stopServiceDiscovery(listener:NsdManager.DiscoveryListener){}
        @Implementation fun resolveService(info:NsdServiceInfo,listener:NsdManager.ResolveListener){resolves+=info to listener}
    }
    private lateinit var discovery:Discovery
    private var peers=emptyList<Peer>()
    private fun info(port:Int=34567)=NsdServiceInfo().apply{serviceName="EZ-peer";serviceType="_ezshare._tcp.";setPort(port);host=InetAddress.getByName("192.168.1.20")}
    @Before fun setup(){NsdShadow.searches.clear();NsdShadow.resolves.clear();discovery=Discovery(ApplicationProvider.getApplicationContext<Context>(),{peers=it},{})}
    @Test fun oldSearchCannotRepopulateStoppedOrRestartedDiscovery(){discovery.discover();val old=NsdShadow.searches.last();old.onServiceFound(info());val resolve=NsdShadow.resolves.last().second;discovery.discover();resolve.onServiceResolved(info());old.onServiceFound(info());assertTrue(peers.isEmpty());assertEquals(1,NsdShadow.resolves.size)}
    @Test fun serviceLostInvalidatesOutstandingResolution(){discovery.discover();val search=NsdShadow.searches.last();search.onServiceFound(info());search.onServiceLost(info());NsdShadow.resolves.last().second.onServiceResolved(info());assertTrue(peers.isEmpty())}
    @Test fun simultaneousResolvesAreSerializedAndUseFreshPort(){discovery.discover();val search=NsdShadow.searches.last();search.onServiceFound(info(30001));search.onServiceFound(info(30002));assertEquals(1,NsdShadow.resolves.size);NsdShadow.resolves[0].second.onServiceResolved(info(30001));assertTrue(peers.isEmpty());assertEquals(2,NsdShadow.resolves.size);NsdShadow.resolves[1].second.onServiceResolved(info(30002));assertEquals("192.168.1.20:30002",peers.single().address)}
    @Test fun restartWaitsForOldResolverBeforeResolvingNewSearch(){discovery.discover();val old=NsdShadow.searches.last();old.onServiceFound(info(30001));discovery.discover();val fresh=NsdShadow.searches.last();fresh.onServiceFound(info(30002));assertEquals(1,NsdShadow.resolves.size);NsdShadow.resolves[0].second.onServiceResolved(info(30001));assertTrue(peers.isEmpty());assertEquals(2,NsdShadow.resolves.size);NsdShadow.resolves[1].second.onServiceResolved(info(30002));assertEquals("192.168.1.20:30002",peers.single().address)}

}
