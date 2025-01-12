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
import { SlottableElement } from "./slottable-element.js";

export default class FileEditor extends SlottableElement {

	static get observedAttributes() {
		return ["data-path", "slot"];
	}

	static get templateName() {
		return "file-editor";
	}

	get state() {
		let i = 0;
		for (let el = this.parentElement.firstElementChild; el !== this; el = el.nextElementSibling)
			i++;
		return this.closest("multi-content").state.contents[i];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("FileEditor.connectedCallback");
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		// console.log("FileEditor.disconnectedCallback");
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleSubmit = async event => {
		// console.log("FileEditor.handleSubmit", event);
		event.preventDefault();
		event.stopPropagation();
		this.state.text = await (await fetch(event.target.action, {
			method: event.target.method,
			headers: { "content-type": "text/plain" },
			body: new FormData(event.target).get("text")
		})).text();
		this.requestUpdate();
	}

	async updateDisplay() {
		// console.log("FileEditor.updateDisplay");
		const s = this.state;
		if (s)
			s.text ??= s.path ? await (await fetch(`/api/files/${s.path}`)).text() : null;
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.state
		}));
	}

	async computeState() {
		// console.log("FileEditor.computeState");
		const s = this.state;
		s.text = await (await fetch(`/api/files/${s.path}`)).text();
	}

	renderState() {
		// console.log("FileEditor.renderState");
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.state
		}));
	}
}
