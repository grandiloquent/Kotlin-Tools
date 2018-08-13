package psycho.euphoria.tools.commons


import android.app.Activity
import android.support.v7.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.dialog_write_permission.view.*
import kotlinx.android.synthetic.main.dialog_write_permission_otg.view.*
import psycho.euphoria.tools.R

class WritePermissionDialog(activity: Activity, val isOTG: Boolean, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val layout = if (isOTG) R.layout.dialog_write_permission_otg else R.layout.dialog_write_permission
        val view = activity.layoutInflater.inflate(layout, null)

        val glide = Glide.with(activity)
        val crossFade = DrawableTransitionOptions.withCrossFade()
        if (isOTG) {
            glide.load(R.mipmap.img_write_storage_otg).transition(crossFade).into(view.write_permissions_dialog_otg_image)
        } else {
            glide.load(R.mipmap.img_write_storage).transition(crossFade).into(view.write_permissions_dialog_image)
            glide.load(R.mipmap.img_write_storage_sd).transition(crossFade).into(view.write_permissions_dialog_image_sd)
        }

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
