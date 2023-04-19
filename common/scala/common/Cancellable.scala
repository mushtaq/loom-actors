package common

trait Cancellable:
  def cancel(): Boolean
