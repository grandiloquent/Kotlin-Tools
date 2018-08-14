package psycho.euphoria.common


import android.app.Activity
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_write_permission.view.*
import kotlinx.android.synthetic.main.dialog_write_permission_otg.view.*
import psycho.euphoria.common.extension.setupDialogStuff
import psycho.euphoria.tools.R

class WritePermissionDialog(activity: Activity, val isOTG: Boolean, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val layout = if (isOTG) R.layout.dialog_write_permission_otg else R.layout.dialog_write_permission
        val view = activity.layoutInflater.inflate(layout, null)



        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
                .setOnCancelListener { CustomActivity.funAfterSAFPermission = null }
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.confirm_storage_access_title)
                }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}
