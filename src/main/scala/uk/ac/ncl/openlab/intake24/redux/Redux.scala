package uk.ac.ncl.openlab.intake24.redux

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

trait Store extends js.Object {

  def getState(): js.Dynamic

  def dispatch(action: js.Any): js.Any

  def subscribe(listener: js.Function): js.Function

  def replaceReducer(nextReducer: js.Function): Unit

}

@js.native
trait Redux extends js.Object {

  def createStore(reducer: js.Function, initialState: js.Dynamic = ???, enhancer: js.Dynamic = ???): Store = js.native

  def combineReducers(reducers: js.Dictionary[js.Function]): js.Function = js.native

  def applyMiddleware(middlewares: js.Function*): js.Function = js.native

  def bindActionCreators(actionCreators: js.Function, dispatch: js.Function): js.Function = js.native

  def compose(functions: js.Function*): js.Function = js.native
}

@js.native
@JSImport("redux", JSImport.Default)
object Redux extends Redux