package net.mypapit.mobile.myrepeater.car

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem

/**
 * Builds what the car is asked to show. Every result is the same kind of
 * template, a map with content beside it, so moving between them never looks
 * like a new screen to the car.
 */
class NearbyTemplates(private val carContext: CarContext) {

    fun loading(): Template = withMap(ListTemplate.Builder().setHeader(header()).setLoading(true).build())

    fun noData(): Template =
        withMap(MessageTemplate.Builder(carContext.getString(R.string.car_no_data)).setHeader(header()).build())

    /**
     * Asking for a permission sends the driver to the phone, so the button
     * works only when parked and the message says to look only when safe.
     */
    fun needLocation(onAllow: () -> Unit): Template {
        val allow = Action.Builder()
            .setTitle(carContext.getString(R.string.car_location_allow))
            .setOnClickListener(ParkedOnlyOnClickListener.create(onAllow))
            .build()
        return withMap(
            MessageTemplate.Builder(carContext.getString(R.string.car_location_off))
                .setHeader(header())
                .addAction(allow)
                .build(),
        )
    }

    fun list(items: List<RepeaterListItem>, focusedId: Long?, onFocus: (Long) -> Unit): Template {
        val list = ItemList.Builder().setNoItemsMessage(carContext.getString(R.string.car_none_in_range))
        for (item in items) {
            val focused = item.repeater.id == focusedId
            val icon = CarIcon.Builder(
                IconCompat.createWithResource(carContext, if (focused) R.drawable.ic_navigation else R.drawable.ic_car_pin),
            ).setTint(if (focused) CarColor.YELLOW else CarColor.DEFAULT).build()

            val row = Row.Builder()
                .setTitle(CarRowText.title(item.repeater))
                .setImage(icon, Row.IMAGE_TYPE_ICON)
                .setOnClickListener { onFocus(item.repeater.id) }
            tuning(item).takeIf { it.isNotEmpty() }?.let(row::addText)
            CarRowText.whereabouts(item).takeIf { it.isNotEmpty() }?.let(row::addText)
            list.addItem(row.build())
        }
        return withMap(ListTemplate.Builder().setHeader(header()).setSingleList(list.build()).build())
    }

    fun tuning(item: RepeaterListItem): String =
        CarRowText.tuning(item.repeater) { carContext.getString(R.string.car_tone, it) }

    private fun withMap(content: Template): Template =
        MapWithContentTemplate.Builder().setContentTemplate(content).build()

    private fun header(): Header = Header.Builder()
        .setTitle(carContext.getString(R.string.car_title))
        .setStartHeaderAction(Action.APP_ICON)
        .build()
}
