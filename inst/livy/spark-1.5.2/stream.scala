//
// This file was automatically generated using livy_sources_refresh()
// Changes to this file will be reverted.
//

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import scala.collection.mutable.HashMap
import scala.language.existentials

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.ChannelHandler.Sharable

import Serializer._

object StreamHandler {

  def read(
    msg: Array[Byte],
    classMap: Map[String, Object],
    logger: Logger,
    hostContext: String): Array[Byte] = {

    val bis = new ByteArrayInputStream(msg)
    val dis = new DataInputStream(bis)

    val bos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(bos)

    val objId = readString(dis)
    val isStatic = readBoolean(dis)
    val methodName = readString(dis)
    val numArgs = readInt(dis)

    if (objId == "Handler") {
      methodName match {
        case "echo" =>
          val args = readArgs(numArgs, dis)
          if (numArgs != 1) throw new IllegalArgumentException("echo should take a single argument")

          writeInt(dos, 0)
          writeObject(dos, args(0))
        case "rm" =>
          try {
            val t = readObjectType(dis)
            if (t != 'c') throw new IllegalArgumentException("object removal expects a string")
            val objToRemove = readString(dis)
            JVMObjectTracker.remove(objToRemove)
            writeInt(dos, 0)
            writeObject(dos, null)
          } catch {
            case e: Exception =>
              logger.logError(s"failed to remove $objId", e)
              writeInt(dos, -1)
              writeString(dos, s"Removing $objId failed: ${e.getMessage}")
          }
        case "getHostContext" =>
          writeInt(dos, 0)
          writeObject(dos, hostContext.asInstanceOf[AnyRef])
        case _ =>
          dos.writeInt(-1)
          writeString(dos, s"Error: unknown method $methodName")
      }
    } else {
      handleMethodCall(isStatic, objId, methodName, numArgs, dis, dos, classMap, logger)
    }

    bos.toByteArray
  }

  def handleMethodCall(
    isStatic: Boolean,
    objId: String,
    methodName: String,
    numArgs: Int,
    dis: DataInputStream,
    dos: DataOutputStream,
    classMap: Map[String, Object],
    logger: Logger): Unit = {
      var obj: Object = null
      try {
        val cls = if (isStatic) {
          if (classMap != null && classMap.contains(objId)) {
            obj = classMap(objId)
            classMap(objId).getClass.asInstanceOf[Class[_]]
          }
          else {
            Class.forName(objId)
          }
        } else {
          JVMObjectTracker.get(objId) match {
            case None => throw new IllegalArgumentException("Object not found " + objId)
            case Some(o) =>
              obj = o
              o.getClass
          }
        }

        val args = readArgs(numArgs, dis)
        val res = Invoke.invoke(cls, objId, obj, methodName, args, logger)

        writeInt(dos, 0)
        writeObject(dos, res.asInstanceOf[AnyRef])
      } catch {
        case e: Exception =>
          logger.logError(s"failed calling $methodName on $objId")
          writeInt(dos, -1)
          writeString(dos, Utils.exceptionString(
            if (e.getCause == null) e else e.getCause
          ))
        case e: NoClassDefFoundError =>
          logger.logError(s"failed calling $methodName on $objId with no class dound error")
          writeInt(dos, -1)
          writeString(dos, Utils.exceptionString(
            if (e.getCause == null) e else e.getCause
          ))
      }
    }

  // Read a number of arguments from the data input stream
  def readArgs(numArgs: Int, dis: DataInputStream): Array[java.lang.Object] = {
    (0 until numArgs).map { _ =>
      readObject(dis)
    }.toArray
  }
}
