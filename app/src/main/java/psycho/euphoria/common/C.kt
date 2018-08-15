package psycho.euphoria.common
import android.os.Build
val audioExtensions: Array<String> get() = arrayOf(".mp3", ".wav", ".wma", ".ogg", ".m4a", ".opus", ".flac", ".aac")
val photoExtensions: Array<String> get() = arrayOf(".jpg", ".png", ".jpeg", ".bmp", ".webp")
val rawExtensions: Array<String> get() = arrayOf(".dng", ".orf", ".nef")
val videoExtensions: Array<String> get() = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".3gpp")
val archiveExtensions: Array<String> get() = arrayOf(".zip", ".rar", ".7z")
const val OTG_PATH = "otg:/"
const val SORT_BY_ARTIST = 4096
const val SORT_BY_DATE_MODIFIED = 2
const val SORT_BY_DATE_TAKEN = 8
const val SORT_BY_DURATION = 8192
const val SORT_BY_EXTENSION = 16
const val SORT_BY_FIRST_NAME = 128
const val SORT_BY_MIDDLE_NAME = 256
const val SORT_BY_NAME = 1
const val SORT_BY_NUMBER = 64
const val SORT_BY_PATH = 32
const val SORT_BY_SIZE = 4
const val SORT_BY_SURNAME = 512
const val SORT_BY_TITLE = 2048
const val SORT_DESCENDING = 1024
const val SORT_ORDER = "sort_order"
const val MIMETYPE_DRM_MESSAGE = "application/vnd.oma.drm.message"
const val REQUEST_OPEN_DOCUMENT_TREE = 1000
const val KEY_PATH = "path"
val isBasePlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BASE
val isBase11Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BASE_1_1
val isCupcakePlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE
val isCurDevelopmentPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUR_DEVELOPMENT
val isDonutPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT
val isEclairPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR
val isEclair01Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_0_1
val isEclairMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1
val isFroyoPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO
val isGingerbreadPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
val isGingerbreadMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1
val isHoneycombPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
val isHoneycombMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
val isHoneycombMr2Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2
val isIceCreamSandwichPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
val isIceCreamSandwichMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
val isJellyBeanPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
val isJellyBeanMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
val isJellyBeanMr2Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
val isKitkatPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
val isKitkatWatchPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
val isLollipopPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
val isLollipopMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
val isMPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
val isNPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
val isNMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
val isOPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
val isOMr1Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
val isLessBase = Build.VERSION.SDK_INT < Build.VERSION_CODES.BASE
val isLessBase11 = Build.VERSION.SDK_INT < Build.VERSION_CODES.BASE_1_1
val isLessCupcake = Build.VERSION.SDK_INT < Build.VERSION_CODES.CUPCAKE
val isLessCurDevelopment = Build.VERSION.SDK_INT < Build.VERSION_CODES.CUR_DEVELOPMENT
val isLessDonut = Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT
val isLessEclair = Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR
val isLessEclair01 = Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR_0_1
val isLessEclairMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR_MR1
val isLessFroyo = Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO
val isLessGingerbread = Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD
val isLessGingerbreadMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1
val isLessHoneycomb = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
val isLessHoneycombMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1
val isLessHoneycombMr2 = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2
val isLessIceCreamSandwich = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH
val isLessIceCreamSandwichMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
val isLessJellyBean = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
val isLessJellyBeanMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1
val isLessJellyBeanMr2 = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
val isLessKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
val isLessKitkatWatch = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH
val isLessLollipop = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
val isLessLollipopMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1
val isLessM = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
val isLessN = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
val isLessNMr1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1
val isLessO = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
/*
BASE = 1
BASE_1_1 = 2
CUPCAKE = 3
DONUT = 4
ECLAIR = 5
ECLAIR_0_1 = 6
ECLAIR_MR1 = 7
FROYO = 8
GINGERBREAD = 9
GINGERBREAD_MR1 = 10
HONEYCOMB = 11
HONEYCOMB_MR1 = 12
HONEYCOMB_MR2 = 13
ICE_CREAM_SANDWICH = 14
ICE_CREAM_SANDWICH_MR1 = 15
JELLY_BEAN = 16
JELLY_BEAN_MR1 = 17
JELLY_BEAN_MR2 = 18
KITKAT = 19
KITKAT_WATCH = 20
L = 21
LOLLIPOP = 21
LOLLIPOP_MR1 = 22
M = 23
N = 24
N_MR1 = 25
O = 26
O_MR1 = 27
P = 28
 */
object C {
    const val TIME_UNSET = Long.MIN_VALUE + 1
    const val INDEX_UNSET = -1
    const val TIME_END_OF_SOURCE = java.lang.Long.MIN_VALUE
    inline fun atLeast(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT >= sdk) f1()
        else f2.invoke()
    }
    inline fun atMost(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT <= sdk) f1()
        else f2.invoke()
    }
    inline fun more(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT > sdk) f1()
        else f2.invoke()
    }
}