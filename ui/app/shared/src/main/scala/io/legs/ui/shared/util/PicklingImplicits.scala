package io.legs.ui.shared.util

import io.legs.CoordinatorStatistics
import io.legs.library.{JobStatus, Priority, JobType, Job}
import io.legs.ui.shared.model.ScheduledJob
import upickle.{Js, ReadWriter, Reader, Writer}


trait PredefPicklingImplicits  {

//	implicit def enumerationWR[E <: Enumeration]: Writer[E#Value] with Reader[E#Value] = ReadWriter[E#Value]({
//		case s : E#Value => Js.Str(s.toString)
//	},{
//		case
//	})

//	implicit def coordinatorStatusRW: Writer[CoordinatorStatistics] with Reader[CoordinatorStatistics] = ReadWriter[CoordinatorStatistics]({
//		case CoordinatorStatistics(jobsCompleted, jobsSucceeded,jobsFailed,lastWorkQueueCheck) =>
//			val _lastWorkQueueCheck = lastWorkQueueCheck.map(d=>Js.Num(d.getMillis)).getOrElse(Js.Null)
//			Js.Obj("jobsCompleted" -> Js.Num(jobsCompleted), "jobsSucceeded" -> Js.Num(jobsSucceeded), "jobsFailed" -> Js.Num(jobsFailed),"lastWorkQueueCheck" -> _lastWorkQueueCheck)
//	}, {
//		case js : Js.Obj =>
//			val values = js.value.toMap
//			val _lastWorkQueueCheck = values.isDefinedAt("lastWorkQueueCheck") && values("lastWorkQueueCheck") != Js.Null match {
//				case true => Some(new org.joda.time.DateTime(values("lastWorkQueueCheck").asInstanceOf[Js.Num].value))
//				case false => None
//			}
//			CoordinatorStatistics(
//				values("jobsCompleted").asInstanceOf[Js.Num].value.toInt,
//				values("jobsSucceeded").asInstanceOf[Js.Num].value.toInt,
//				values("jobsFailed").asInstanceOf[Js.Num].value.toInt,
//				_lastWorkQueueCheck
//			)
//	})

//	implicit def jobWR : Writer[Job] with Reader[Job] = ReadWriter[Job]({
//		case Job()
//	},{
//	})
//
//
//	implicit def scheduledJobRW: Writer[ScheduledJob] with Reader[ScheduledJob] = ReadWriter[ScheduledJob]({
//		case ScheduledJob(jobId,schedule,jobData) =>
//			Js.Obj("jobId" -> Js.Str(jobId), "schedule" -> Js.Str(schedule), "jobData" -> ???)
//	}, {
//		case js : Js.Obj =>
//			val values = js.value.toMap
//			ScheduledJob(
//				values("jobId").asInstanceOf[Js.Str].value,
//				values("schedule").asInstanceOf[Js.Str].value,
//
//			)
//	})

	implicit def jobTypeWR : Writer[JobType.Value] with Reader[JobType.Value] = ReadWriter[JobType.Value]({
		case zValue : JobType.Value => Js.Str(zValue.toString)
	},{
		case Js.Str(zValueStr) => JobType.withName(zValueStr)
	})

	implicit def priorityWR : Writer[Priority.Value] with Reader[Priority.Value] = ReadWriter[Priority.Value]({
		case zValue : Priority.Value => Js.Str(zValue.toString)
	},{
		case Js.Str(zValueStr) => Priority.withName(zValueStr)
	})

	implicit def jobStatusWR : Writer[JobStatus.Value] with Reader[JobStatus.Value] = ReadWriter[JobStatus.Value]({
		case zValue : JobStatus.Value => Js.Str(zValue.toString)
	},{
		case Js.Str(zValueStr) => JobStatus.withName(zValueStr)
	})


//	implicit def jobWR: Writer[Job] with Reader[Job] = ReadWriter[Job]({
//		case Job(instructions,labels,input,description,jobType,priority,id,status,parentId,retries,lastRunTime,creationTime,uuid) =>
//			Js.Obj(
//				"instructions" -> Js.Str(instructions),
//				"labels" -> Js.Arr(labels.map(Js.Str) : _*),
//				"input" -> input
//			)
//	}, {
//		case js : Js.Obj =>
//			val values = js.value.toMap
//			Job(
//				values("instructions").asInstanceOf[Js.Str].value,
//
//			)
//			ScheduledJob(
//				values("jobId").asInstanceOf[Js.Str].value,
//				values("schedule").asInstanceOf[Js.Str].value,
//
//			)
//	})

}

object PicklingImplicits extends PredefPicklingImplicits {




}
