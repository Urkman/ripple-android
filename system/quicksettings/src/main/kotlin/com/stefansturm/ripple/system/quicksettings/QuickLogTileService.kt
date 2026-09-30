package com.stefansturm.ripple.system.quicksettings

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickLogTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "Ripple"
            contentDescription = "Log the default Ripple amount"
            state = Tile.STATE_ACTIVE
            icon = Icon.createWithResource(this@QuickLogTileService, android.R.drawable.ic_input_add)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent("de.stefansturm.ripple.LOG_DEFAULT").apply {
            setClassName(packageName, "com.stefansturm.ripple.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivityAndCollapse(intent)
    }
}
