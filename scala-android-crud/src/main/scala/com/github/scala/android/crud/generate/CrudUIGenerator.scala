package com.github.scala.android.crud.generate

import com.github.triangle._
import scala.tools.nsc.io.Path
import xml._
import com.github.scala.android.crud.{NamingConventions, CrudApplication, CrudType}
import com.github.scala.android.crud.common.Common
import collection.immutable.List
import com.github.scala.android.crud.persistence.EntityType

/** A UI Generator for a CrudTypes.
  * @author Eric Pabst (epabst@gmail.com)
  */

object CrudUIGenerator extends Logging {
  protected def logTag = Common.logTag
  private val lineSeparator = System.getProperty("line.separator")
  private[generate] val prettyPrinter = new PrettyPrinter(80, 2) {
    override protected def traverse(node: Node, namespace: NamespaceBinding, indent: Int) {
      node match {
        case Text(text) if text.trim().size == 0 => super.traverse(Text(text.trim()), namespace, indent)
        case n: Elem if n.child.size == 0 => makeBox(indent, leafTag(n))
        case _ => super.traverse(node, namespace, indent)
      }
    }
  }

  private def writeXmlToFile(path: Path, xml: Node) {
    val file = path.toFile
    file.parent.createDirectory()
    file.writeAll("""<?xml version="1.0" encoding="utf-8"?>""", lineSeparator, prettyPrinter.format(xml))
    println("Wrote " + file)
  }

  def generateAndroidManifest(application: CrudApplication): Node = {
    val activityNames = application.allCrudTypes.flatMap { entity =>
      List(entity.listActivityClass.getSimpleName, entity.activityClass.getSimpleName)
    }
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
              package={application.packageName}
              android:versionName="${project.version}"
              android:versionCode="${versionCode}">
      <application android:label="@string/app_name" android:icon="@drawable/icon"
                   android:theme="@android:style/Theme.NoTitleBar"
                   android:debuggable="true"
                   android:backupAgent={application.classNamePrefix + "BackupAgent"} android:restoreAnyVersion="true">
        <meta-data android:name="com.google.android.backup.api_key"
                   android:value="TODO: get a backup key from http://code.google.com/android/backup/signup.html and put it here."/>
        <activity android:name={"." + activityNames.head} android:label="@string/app_name">
          <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
          </intent-filter>
        </activity>
        {activityNames.tail.map { name => <activity android:name={"." + name} android:label="@string/app_name"/>}}
      </application>
      <uses-sdk android:minSdkVersion="8"/>
    </manifest>
  }

  def generateValueStrings(crudType: CrudType): NodeSeq = {
    <string name={crudType.entityNameLayoutPrefix + "_list"}>{crudType.entityName} List</string> +: {
      if (attemptToEvaluate(crudType.createAction.isDefined).getOrElse(true)) {
        Seq(<string name={"add_" + crudType.entityNameLayoutPrefix}>Add {crudType.entityName}</string>,
            <string name={"edit_" + crudType.entityNameLayoutPrefix}>Edit {crudType.entityName}</string>)
      } else {
        Nil
      }
    }
  }

  def attemptToEvaluate[T](f: => T): Option[T] =
    try {
      Some(f)
    } catch {
      case e => debug(e.toString); None
    }

  def generateValueStrings(application: CrudApplication): Node = {
    <resources>
      <string name="app_name">{application.name}</string>
      {application.allCrudTypes.flatMap(generateValueStrings(_))}
    </resources>
  }

  def generateLayouts(application: CrudApplication) {
    application.allEntityTypes.foreach(generateLayouts(_))
    writeXmlToFile(Path("AndroidManifest.xml"), generateAndroidManifest(application))
    writeXmlToFile(Path("res") / "values" / "strings.xml", generateValueStrings(application))
  }

  protected[generate] def fieldLayoutForHeader(field: ViewIdFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 2 == 0) "left" else "right"
    val layoutWidth = if (position % 2 == 0) "wrap_content" else "fill_parent"
    <TextView android:text={field.displayName} android:gravity={gravity}
              android:layout_width={layoutWidth}
              android:layout_height="wrap_content"
              android:paddingRight="3sp"
              android:textAppearance={textAppearance}/>
  }

  protected[generate] def fieldLayoutForRow(field: ViewIdFieldInfo, position: Int): NodeSeq = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 2 == 0) "left" else "right"
    val layoutWidth = if (position % 2 == 0) "wrap_content" else "fill_parent"
    val attributes = <TextView android:id={"@+id/" + field.id} android:gravity={gravity}
                               android:layout_width={layoutWidth}
                               android:layout_height="wrap_content"
                               android:paddingRight="3sp"
                               android:textAppearance={textAppearance}/>.attributes
    applyAttributesToHead(field.layout.displayXml, attributes)
  }

  protected def headerLayout(fields: List[ViewIdFieldInfo]) =
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                  android:paddingTop="2dip"
                  android:paddingBottom="2dip"
                  android:paddingLeft="6dip"
                  android:paddingRight="6dip"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:minHeight="?android:attr/listPreferredItemHeight"
                  android:orientation="vertical">{
      fields.grouped(2).map { rowFields =>
        <LinearLayout android:layout_width="match_parent"
                      android:layout_height="wrap_content"
                      android:orientation="horizontal">
          {rowFields.map(field => fieldLayoutForHeader(field, fields.indexOf(field)))}
        </LinearLayout>
      }
    }
    </LinearLayout>

  protected def rowLayout(fields: List[ViewIdFieldInfo]) =
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                  android:paddingTop="2dip"
                  android:paddingBottom="2dip"
                  android:paddingLeft="6dip"
                  android:paddingRight="6dip"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:minHeight="?android:attr/listPreferredItemHeight"
                  android:orientation="vertical">{
      fields.grouped(2).map { rowFields =>
        <LinearLayout android:layout_width="match_parent"
                      android:layout_height="wrap_content"
                      android:orientation="horizontal">
          {rowFields.map(field => fieldLayoutForRow(field, fields.indexOf(field)))}
        </LinearLayout>
      }
    }
    </LinearLayout>

  private def applyAttributesToHead(xml: NodeSeq, attributes: MetaData): NodeSeq = xml.headOption.map {
    case e: Elem => e % attributes
    case x => x
  }.map(_ +: xml.tail).getOrElse(NodeSeq.Empty)

  protected def fieldLayoutForEntry(field: ViewIdFieldInfo, position: Int): Elem = {
    val gravity = "right"
    val textAppearance = "?android:attr/textAppearanceLarge"
    val attributes = <EditText android:id={"@+id/" + field.id}/>.attributes
    <TableRow>
      <TextView android:text={field.displayName + ":"} android:textAppearance={textAppearance} android:gravity={gravity}/>
      {applyAttributesToHead(field.layout.editXml, attributes)}
    </TableRow>
  }

  def entryLayout(fields: List[ViewIdFieldInfo]) = {
    <TableLayout xmlns:android="http://schemas.android.com/apk/res/android"
                 android:layout_width="fill_parent"
                 android:layout_height="wrap_content"
                 android:stretchColumns="1">
      {fields.map(field => fieldLayoutForEntry(field, fields.indexOf(field)))}
    </TableLayout>
  }

  private def writeLayoutFile(name: String, xml: Elem) {
    writeXmlToFile(Path("res") / "layout" / (name + ".xml"), xml)
  }

  def generateLayouts(entityType: EntityType) {
    println("Generating layout for " + entityType)
    val info = EntityTypeViewInfo(entityType)
    val layoutPrefix = NamingConventions.toLayoutPrefix(entityType.entityName)
    writeLayoutFile(layoutPrefix + "_header", headerLayout(info.displayableViewIdFieldInfos))
    writeLayoutFile(layoutPrefix + "_row", rowLayout(info.displayableViewIdFieldInfos))
    if (info.isUpdateable) writeLayoutFile(layoutPrefix + "_entry", entryLayout(info.updateableViewIdFieldInfos))
  }
}
