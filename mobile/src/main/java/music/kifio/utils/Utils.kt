@file:JvmName("ExtensionsUtils")

package music.kifio.utils

import android.content.Context
import android.content.res.XmlResourceParser
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.a_main.*
import kotlinx.android.synthetic.main.a_main.view.*
import music.kifio.R

/**
 * Created by kifio on 01.03.17.
 */

fun ImageView.loadUrl(url: String?) {

    if (url != null) {
        Picasso.with(context).load(url).into(this)
    } else {
        setImageResource(R.drawable.artwork_none)
    }

}

fun RecyclerView.init(context: Context, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
    setAdapter(adapter)
    list.layoutManager = LinearLayoutManager(context)
    list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
}

fun parseClientId(parser: XmlResourceParser) : String {

    var eventType = -1

    while (eventType != XmlResourceParser.END_DOCUMENT) {

        if (eventType == XmlResourceParser.START_TAG) {

            if (parser.name == "credentials") {
                return parser.getAttributeValue(null, "id")
            }

        }

        eventType = parser.next()

    }

    return ""
}