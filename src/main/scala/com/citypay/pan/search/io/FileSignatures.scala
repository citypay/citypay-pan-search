package com.citypay.pan.search.io

import com.citypay.pan.search.util.Hex
import net.sf.sevenzipjbinding.ArchiveFormat


object FileSignatures {

  def sig(name: String, extensions: String, str: String, offset: Int): FileSignature =
    FileSignature(name, extensions.split(",").toList, Hex.fromHex(str), offset)

  def sig(str: String, offset: Int = 0): FileSignature = {
    val i = str.indexOf(":")
    val j = str.indexOf(":", i + 1)
    sig(str.substring(0, i), str.substring(i, j), str.substring(j), offset)
  }

  // list of file signatures to map to a file review process, only common items with header values are created
  // formats such as RIFF do not check later in the document as these are unlikely to expose data
  val List = Map(
    sig("Lempel-Ziv-Welch:z,tar.z:1F 9D") -> ArchiveFormat.LZMA,
    sig("LZH:z,tar.z:1F A0") -> ArchiveFormat.LZH,
    sig("Bzip2:bz2:42 5A 68") -> ArchiveFormat.BZIP2,
    sig("GIF87a:gif:47 49 46 38 37 61") -> None, // image file, exempt from scan
    sig("GIF89a:gif:47 49 46 38 39 61") -> None, // image file, exempt from scan
    sig("TIF LE:tif,tiff:49 49 2A 00") -> None, // image file, exempt from scan
    sig("TIF BE:tif,tiff:4D 4D 00 2A") -> None, // image file, exempt from scan
    sig("Better Portable Graphics format:bpg:42 50 47 FB") -> None, // image file, exempt from scan
    sig("JPEG raw:jpg,jpeg:FF D8 FF DB") -> None, // image file, exempt from scan
    sig("JPEG Jfif:jpg,jpeg:FF D8 FF E0") -> None, // image file, exempt from scan
    sig("JPEG Exif:jpg,jpeg:FF D8 FF E1") -> None, // image file, exempt from scan
    sig("JPEG Exif:jpg,jpeg:FF D8 FF E1") -> None, // image file, exempt from scan
    sig("lzip:lz:4C 5A 49 50") -> ArchiveFormat.LZMA,
    sig("zip:zip,jar,odt,ods,odp,docx,xlsx,pptx,vsdx,apk:50 4B 03 04") -> ArchiveFormat.ZIP,
    sig("zip (empty archive):zip,jar,odt,ods,odp,docx,xlsx,pptx,vsdx,apk:50 4B 05 06") -> ArchiveFormat.ZIP,
    sig("zip (spanned archive):zip,jar,odt,ods,odp,docx,xlsx,pptx,vsdx,apk:50 4B 07 08") -> ArchiveFormat.ZIP,
    sig("RAR:rar:52 61 72 21 1A 07 00") -> ArchiveFormat.RAR,
    sig("RAR 5:rar:52 61 72 21 1A 07 01 00") -> ArchiveFormat.RAR,
    sig("Portable Network Graphics format:png:89 50 4E 47 0D 0A 1A 0A") -> None, // image file, exempt from scan
    sig("Java class file:class:CA FE BA BE") -> 1,
    sig("Unicode file::EF BB BF") -> 1,
    sig("PostScript document:ps:25 21 50 53") -> None, // postscript is deemed as rendered rather than containing text
    sig("PDF document:pdf:25 50 44 46") -> None, //todo improve PDF scanning such as itext library, not currently supported
    sig("Adobe encapsulated PostScript:eps:C5 D0 D3 C6") -> None,
    sig("Encapsulated PostScript file:eps:25 21 50 53 2D 41 64 6F") -> None,
    sig("Advanced Systems Format:asf,wma,wmv:30 26 B2 75 8E 66 CF 11 A6 D9 00 AA 00 62 CE 6C") -> None, // media files, exempt
    sig("Ogg:ogg,oga,ogv:4F 67 67 53") -> None, // media files, exempt
    sig("Photoshop Document File:psd:38 42 50 53") -> None, // media files, exempt
    sig("Waveform Audio File:wav:52 49 46 46") -> None, // nn nn nn nn 57 41 56 4 // media files, exempt
    sig("Audio Vide Interleave:avi:52 49 46 46") -> None, // nn nn nn nn 41 56 49 2 // media files, exempt
    sig("MP3 file without ID3:mp3:FF FB") -> None, // media files, exempt
    sig("MP3 file ID3v2:mp2:49 44 33") -> None, // media files, exempt
    sig("Bitmap:bmp,dib:42 4D") -> None, // image files, exempt
    sig("ISO9660 Image:iso:43 44 30 30 31") -> None, // image file for cd/dvd exempt
    sig("Free Lossless Audio Codec:flac:66 4C 61 43") -> None, // media files, exempt
    sig("MIDI sound file:mid,midi:4D 54 68 64") -> None, // media files, exempt
    sig("Compound File Binary Format:doc,xls,ppt,msg:D0 CF 11 E0 A1 B1 1A E1") -> 1, //todo will perform a basic scan, can be improved using apache
    sig("VMDK:cmdk:4B 44 4D") -> None, // vmware exempt
    sig("Apple Disk Image format:dmg:78 01 73 0D 62 62 60") -> None, // usde for installation media, exempt
    sig("tar archive:tar:75 73 74 61 72 00 30 30") -> ArchiveFormat.TAR,
    sig("tar archive:tar:75 73 74 61 72 20 20 00") -> ArchiveFormat.TAR,
    sig("7-Zip File Format:7z:37 7A BC AF 27 1C") -> ArchiveFormat.SEVEN_ZIP,
    sig("GZIP:gz,tar.gz:1F 8B") -> ArchiveFormat.GZIP,
    sig("LZ4 Frame Format:lz4:04 22 4D 18") -> ArchiveFormat.LZH,
    sig("Microsoft Cabinet File:cab:4D 53 43 46") -> None, // os level file, exempt
    sig("Microsoft compress Quantum format:exe,ex_:53 5A 44 44 88 F0 27 33") -> None, // exe file, exempt
    sig("Free Lossless Image Format:flif:46 4C 49 46") -> None, // media file, exempt
    sig("DER encoded X.509 certificae:der:30 82") -> None, // exempt
    sig("DICOM Medical File Format:dcm:44 49 43 4D") -> None, // exempt
    sig("WOFF File Format 1.0:woff:77 4F 46 46") -> None, // exempt
    sig("WOFF File Format 2.0:woff:77 4F 46 32") -> None, // exempt
    sig("eXtensible Markup Language:xml:3c 3f 78 6d 6c 20") -> 1,
    sig("WebAssembly binary format:wasm:6d 73 61 00") -> None, //exempt
    sig("Lepton compressed JPEG:lep:cf 84 01") -> None, // media file exempt
    sig("Flash SWF:swf:43 57 53") -> None, // media file exempt
    sig("Flash SWF:swf:46 57 53") -> None, // media file exempt
    sig("Linux deb file:deb:21 3C 61 72 63 68 3E") -> None, // os installation file exempt
    sig("U-Boot::27 05 19 56") -> None // os installation file exempt
  )

  val ByteLen: Int = List.foldLeft(0)((i, f) => if (f._1.signature.length > i) f._1.signature.length else i)

}
