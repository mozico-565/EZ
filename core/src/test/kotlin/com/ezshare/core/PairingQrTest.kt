package com.ezshare.core
import org.junit.Assert.*
import org.junit.Test
class PairingQrTest{
    private val now=1000L
    private val valid=PairingQr("192.168.43.1",33045,"a".repeat(32),1500)
    @Test fun qrIsOfflineVersionedAndRoundTrips(){assertEquals(valid,PairingQr.decode(valid.encode(),now))}
    @Test fun qrRejectsInternetAndUnexpectedSchemes(){listOf("8.8.8.8","127.0.0.1","0.0.0.0","255.255.255.255","192.168.1.999").forEach{assertThrows(Exception::class.java){PairingQr.decode(valid.copy(host=it).encode(),now)}};assertThrows(Exception::class.java){PairingQr.decode(valid.encode().replace("ezpair://","https://"),now)}}
    @Test fun qrRejectsExpiredFutureDuplicateUnknownOrOversizedData(){listOf(valid.copy(expires=999).encode(),valid.copy(expires=1601).encode(),valid.encode()+"&host=10.0.0.1",valid.encode()+"&extra=x",valid.encode()+"#x","x".repeat(513),valid.copy(port=80).encode(),valid.copy(nonce="secret").encode(),valid.encode().replace("v2","v1")).forEach{assertThrows(Exception::class.java){PairingQr.decode(it,now)}}}
    @Test fun hotspotAndPrivateAddressesAreAccepted(){listOf("10.0.0.1","172.16.0.1","172.31.255.1","192.168.49.1","169.254.1.8").forEach{assertEquals(it,PairingQr.decode(valid.copy(host=it).encode(),now).host)}}
}
