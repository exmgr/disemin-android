package gr.exm.agroxm.ui.common

import android.app.Activity
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

@IntDef(value = [Toast.LENGTH_SHORT, Toast.LENGTH_LONG])
@Retention(AnnotationRetention.SOURCE)
annotation class Duration

fun Activity?.toast(
    @StringRes message: Int,
    vararg args: Any = emptyArray(),
    @Duration duration: Int = Toast.LENGTH_SHORT
) {
    this?.toast(this.getString(message, *args), duration)
}

fun Activity?.toast(
    message: String,
    @Duration duration: Int = Toast.LENGTH_SHORT
) {
    this?.let {
        Toast.makeText(it, message, duration).show()
    }
}

fun Fragment?.toast(
    @StringRes message: Int,
    vararg args: Any = emptyArray(),
    @Duration duration: Int = Toast.LENGTH_SHORT
) {
    this?.toast(this.getString(message, *args), duration)
}

fun Fragment?.toast(message: String, @Duration duration: Int = Toast.LENGTH_SHORT) {
    this?.let { it.activity.toast(message, duration) }
}

fun AppCompatActivity.addFragment(
    fragment: Fragment,
    tag: String,
    frameId: Int,
    addToBackStack: Boolean = false
) {
    val fragmentExists = supportFragmentManager.findFragmentByTag(tag)
    if (fragmentExists != null) {
        replaceFragment(fragmentExists, tag, frameId, addToBackStack)
    } else {
        if (addToBackStack) {
            supportFragmentManager.inTransaction { add(frameId, fragment, tag).addToBackStack(tag) }
        } else {
            supportFragmentManager.inTransaction { add(frameId, fragment, tag) }
        }
    }
}

fun AppCompatActivity.replaceFragment(
    fragment: Fragment,
    tag: String,
    frameId: Int,
    addToBackStack: Boolean = true
) {
    val fragmentExists = supportFragmentManager.findFragmentByTag(tag)
    if (fragmentExists != null) {
        supportFragmentManager.inTransaction {
            replace(frameId, fragmentExists, tag)
                .also { if (addToBackStack) addToBackStack(tag) }
        }
    } else {
        supportFragmentManager.inTransaction {
            replace(frameId, fragment, tag)
                .also { if (addToBackStack) addToBackStack(tag) }
        }
    }
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()
