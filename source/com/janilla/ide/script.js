/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import DirectoryContent from "./directory-content.js";
import EditorList from "./editor-list.js";
import EntryList from "./entry-list.js";
import EntryTree from "./entry-tree.js";
import FileContent from "./file-content.js";
import FileExecution from "./file-execution.js";
import OutputArea from "./output-area.js";
import RootLayout from "./root-layout.js";

customElements.define("directory-content", DirectoryContent);
customElements.define("editor-list", EditorList);
customElements.define("entry-list", EntryList);
customElements.define("entry-tree", EntryTree);
customElements.define("file-content", FileContent);
customElements.define("file-execution", FileExecution);
customElements.define("output-area", OutputArea);
customElements.define("root-layout", RootLayout);
