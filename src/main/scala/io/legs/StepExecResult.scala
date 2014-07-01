package io.legs

case class StepExecResult(
	stepYielded: Map[String,Any],
	error: Option[String] = None,
	stateChanges: Option[Map[String,Any]] = None
)
