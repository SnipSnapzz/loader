## Native Payload Loader (`JNICLoader`) — Runtime DLL Extraction & Execution

### Overview

The class `dev.jnic.zTEJeI.JNICLoader` functions as a **custom JNI bootstrapper** responsible for loading a native payload at runtime. Rather than shipping a visible `.dll` or using standard JNI loading mechanisms, it embeds the native binary inside an obfuscated data resource and reconstructs it dynamically during execution.

This design strongly indicates **intentional evasion of static analysis and antivirus detection**.

---

### Embedded Native Payload

The native code is **not present as a standalone DLL** within the JAR. Instead, it is stored as a compressed binary blob at the following path:

```
/dev/jnic/lib/aef01f72-edd4-4756-b2ee-ef47ff66de66.dat
```

This `.dat` file contains **multiple architecture‑specific native binaries** packed together. The loader selects the correct binary slice at runtime based on operating system and CPU architecture.

---

### Platform & Architecture Gating

During static initialization, the loader inspects:

- `os.name`
- `os.arch`

Supported targets include:
- Windows x86_64 / amd64
- Windows ARM64 (aarch64)

If the platform does not match an expected configuration, execution halts with an `UnsatisfiedLinkError`. This prevents execution in analysis environments that do not match the intended victim profile.

---

### Offset‑Based Binary Selection

Rather than naming or tagging binaries, the loader uses **hard‑coded byte offsets** to extract the correct native payload from the `.dat` file.

Each platform configuration defines:
- A **start offset**
- An **end offset**

Only the bytes within that range are reconstructed and written to disk. This allows multiple native payloads to coexist in a single resource while avoiding recognizable file headers in static scans.

---

### Custom Decompression Engine

`JNICLoader` extends `InputStream` and implements a **bespoke decompression routine** rather than using Java’s built‑in `Inflater` or `GZIPInputStream`.

Supporting classes (`W`, `J`, `h`) collectively implement:
- Sliding window buffering
- Dictionary/state management
- Bit‑level decoding logic

The use of a custom decompressor avoids:
- Known compression signatures
- Heuristic detection based on standard libraries
- Straightforward extraction by automated tools

---

### Runtime Extraction & Loading

At runtime, the loader:

1. Creates a temporary file using a randomized name
2. Decompresses the selected native payload slice into that file
3. Marks the file for deletion on JVM exit
4. Loads the binary via:
   ```java
   System.load(tempFile.getAbsolutePath());
   ```

Once loaded, the native code is fully active within the JVM process.

---

### Native ↔ Java Communication Buffer

The loader allocates a direct `ByteBuffer`:

```java
ByteBuffer.allocateDirect(64).order(ByteOrder.LITTLE_ENDIAN);
```

This buffer is populated with constant values and is likely used as:
- A command or state exchange region
- A lightweight integrity or anti‑tamper mechanism
- A shared memory bridge between Java and native code

Because it is a **direct buffer**, it is accessible from native code without copying through the JVM heap.

---

### Security Implications

After successful loading:
- Any `native` methods declared elsewhere in the JAR become callable
- The native code operates outside JVM safety guarantees
- OS‑level APIs become accessible regardless of Java sandboxing

This architecture strongly suggests that **sensitive operations are intentionally delegated to native code** to evade JVM‑level inspection and monitoring.

---

### Key Takeaways

- The Java layer primarily acts as a **loader and orchestrator**
- The `.dat` resource hides the true payload from static scanners
- Custom decompression and offset slicing defeat signature‑based detection
- Native execution enables behavior that would be visible or restricted in pure Java

This stage represents the **final transition from managed code to unrestricted native execution**.
