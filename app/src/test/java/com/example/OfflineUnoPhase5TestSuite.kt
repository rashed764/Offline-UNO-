package com.example

import com.example.game.network.host.LanHostServer
import com.example.game.network.qr.LanQrPayload
import com.example.game.network.qr.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUnoPhase5TestSuite {

  @Test
  fun roomCodeGenerationFormatAndReadability() {
    val code = LanHostServer.generateRoomCode()
    assertEquals("Room code must be exactly 6 characters", 6, code.length)
    assertTrue("Room code must contain only valid letters or digits", code.all { it.isLetterOrDigit() })
    assertFalse("Room code has no lowercase letters", code.any { it.isLowerCase() })
    assertFalse("Room code avoids 0 to prevent confusion", code.contains('0'))
    assertFalse("Room code avoids O to prevent confusion", code.contains('O'))
    assertFalse("Room code avoids 1 to prevent confusion", code.contains('1'))
    assertFalse("Room code avoids I to prevent confusion", code.contains('I'))
  }

  @Test
  fun qrPayloadBuildAndParsePreservesAllFields() {
    val payload = LanQrPayload(
      version = 1,
      roomCode = "AB7K9P",
      hostIp = "192.168.1.150",
      port = 19842,
      password = "secretPassword123",
      entryFee = 500,
      maxPlayers = 4
    )

    val uriString = payload.toUriString()
    assertTrue(uriString.startsWith("OFFLINEUNO://join?"))

    val parsed = LanQrPayload.parse(uriString)
    assertNotNull(parsed)
    assertEquals(1, parsed!!.version)
    assertEquals("AB7K9P", parsed.roomCode)
    assertEquals("192.168.1.150", parsed.hostIp)
    assertEquals(19842, parsed.port)
    assertEquals("secretPassword123", parsed.password)
    assertEquals(500, parsed.entryFee)
    assertEquals(4, parsed.maxPlayers)
  }

  @Test
  fun qrPayloadParseRejectsInvalidSchemesAndMalformedData() {
    assertNull(LanQrPayload.parse("https://random.com/join?room=AB7K9P"))
    assertNull(LanQrPayload.parse("OFFLINEUNO://wrongAuthority?room=AB7K9P"))
    assertNull(LanQrPayload.parse("not an uri"))
    assertNull(LanQrPayload.parse("OFFLINEUNO://join?port=notAnInt"))
    assertNull(LanQrPayload.parse("OFFLINEUNO://join?room=&host=192.168.1.1&port=19842"))
  }

  @Test
  fun qrCodeGeneratorProducesValidNonEmptyBitmap() {
    val payload = LanQrPayload(
      roomCode = "UNO123",
      hostIp = "192.168.1.50",
      port = 19842
    )
    val bitmap = QrCodeGenerator.generateQrBitmap(payload.toUriString(), sizePixels = 200)
    assertNotNull(bitmap)
    assertTrue(bitmap.width >= 150)
    assertTrue(bitmap.height >= 150)
  }
}
