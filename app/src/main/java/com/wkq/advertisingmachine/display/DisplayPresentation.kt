package com.wkq.advertisingmachine.display

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.view.WindowManager
import com.wkq.advertisingmachine.view.LogicalViewportContainer

class DisplayPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    lateinit var container: LogicalViewportContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.apply {
            setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        container = LogicalViewportContainer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.DKGRAY)
        }

        setContentView(container)
    }

    override fun onStop() {
        container.clearAllWindows()
        super.onStop()
    }
}
