import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
import threading
import os
import datetime
import json
from crypto_manager import CryptoManager

# Try to import tkinterdnd2 for Drag and Drop support
try:
    from tkinterdnd2 import DND_FILES, TkinterDnD
    HAS_DND = True
    BaseWindow = TkinterDnD.Tk
except ImportError:
    HAS_DND = False
    BaseWindow = tk.Tk
    print("Warning: tkinterdnd2 not found. Drag and drop will be disabled.")

TRANSLATIONS = {
    "title": {"en": "Python Encryption Tool (PBKDF2 + AES)", "zh": "Python 加密工具 (PBKDF2 + AES)"},
    "control_panel": {"en": "Control Panel", "zh": "控制面板"},
    "password": {"en": "密码:", "zh": "密码:"},
    "confirm_pwd": {"en": "confirm pwd", "zh": "确认密码"},
    "algorithm": {"en": "Algorithm:", "zh": "算法:"},
    "set_enc_out": {"en": "Set Enc Output Dir", "zh": "设置加密输出目录"},
    "set_dec_out": {"en": "Set Dec Output Dir", "zh": "设置解密输出目录"},
    "text_enc_title": {"en": "Text Encryption", "zh": "文本加密"},
    "history": {"en": "History/Results:", "zh": "历史/结果:"},
    "input_text": {"en": "Input Text:", "zh": "输入文本:"},
    "btn_encrypt": {"en": "Encrypt", "zh": "加密"},
    "text_dec_title": {"en": "Text Decryption", "zh": "文本解密"},
    "btn_decrypt": {"en": "Decrypt", "zh": "解密"},
    "file_ops_title": {"en": "File Operations", "zh": "文件操作"},
    "file_enc_title": {"en": "File Encryption", "zh": "文件加密"},
    "drag_info": {"en": "Drag file here or click Select:", "zh": "拖拽文件至此或点击选择:"},
    "select_file": {"en": "Select File", "zh": "选择文件"},
    "btn_enc_file": {"en": "Encrypt File", "zh": "加密文件"},
    "file_dec_title": {"en": "File Decryption", "zh": "文件解密"},
    "btn_dec_file": {"en": "Decrypt File", "zh": "解密文件"},
    "ready": {"en": "Ready", "zh": "就绪"},
    "processing": {"en": "Processing...", "zh": "处理中..."},
    "done": {"en": "Done!", "zh": "完成!"},
    "missing_pwd_title": {"en": "Missing Password", "zh": "缺少密码"},
    "missing_pwd_msg": {"en": "Please enter a password first.", "zh": "请先输入密码。"},
    "pwd_conf_title": {"en": "Password Confirmed", "zh": "密码已确认"},
    "pwd_conf_msg": {"en": "Password captured.", "zh": "密码已捕获。"},
    "pwd_empty_msg": {"en": "Password is empty!", "zh": "密码为空！"},
    "warning": {"en": "Warning", "zh": "警告"},
    "error": {"en": "Error", "zh": "错误"},
    "file_error_title": {"en": "File Error", "zh": "文件错误"},
    "file_error_msg": {"en": "Invalid input file.", "zh": "无效的输入文件。"},
    "op_complete": {"en": "Complete", "zh": "完成"},
    "saved_to": {"en": "Saved to:", "zh": "保存至:"},
    "dec_fail_msg": {"en": "Decryption failed. Check Password or format.", "zh": "解密失败。请检查密码或格式。"},
    "lang_label": {"en": "Language:", "zh": "语言:"},
    "iterations": {"en": "Iterations(0<x<100000000):", "zh": "迭代次数(0<x<100000000):"},
    "confirm_iterations": {"en": "Confirm Iterations", "zh": "确认迭代次数"},
    "missing_iterations_title": {"en": "Missing Iterations", "zh": "缺少迭代次数"},
    "missing_iterations_msg": {"en": "Iterations cannot be empty!", "zh": "迭代次数不能为空！"},
    "iterations_conf_msg": {"en": "Iterations confirmed.", "zh": "迭代次数已确认。"},
    "iterations_empty_msg": {"en": "Iterations is empty!", "zh": "迭代次数为空！"},
    "iterations_max_restrict": {"en": "Iterations cannot exceed 100000000!", "zh": "迭代次数不能超过100000000！"},
}

CONFIG_FILE = "settings.json"

class EncryptApp(BaseWindow):
    # MAX_ITERATIONS
    MAX_ITERATIONS = 100_000_000
    def __init__(self):
        super().__init__()
        self.crypto = CryptoManager()
        self.enc_output_path = tk.StringVar()
        self.dec_output_path = tk.StringVar()
        self.selected_enc_file = tk.StringVar()
        self.selected_dec_file = tk.StringVar()
        self.iterations_var = tk.StringVar()
        
        self.lang_code = "en"
        self.translatable_widgets = [] # List of (widget, key, attribute_name)
        
        # Load settings before UI setup to apply language and paths
        self.load_settings()

        self.setup_ui()
        self.update_language() # Apply language to UI
        
    def load_settings(self):
        """Load settings from JSON file."""
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
                    settings = json.load(f)
                    self.enc_output_path.set(settings.get("enc_output_path", ""))
                    self.dec_output_path.set(settings.get("dec_output_path", ""))
                    self.lang_code = settings.get("language", "en")
                    self.iterations_var.set(settings.get("iterations", "100000"))
            except Exception as e:
                print(f"Failed to load settings: {e}")

    def save_settings(self):
        """Save current settings to JSON file."""
        settings = {
            "enc_output_path": self.enc_output_path.get(),
            "dec_output_path": self.dec_output_path.get(),
            "language": self.lang_code,
            "iterations": int(self.iterations_var.get())
        }
        try:
            with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
                json.dump(settings, f, ensure_ascii=False, indent=4)
        except Exception as e:
            print(f"Failed to save settings: {e}")

    def tr(self, key):
        """Translate a key to the current language."""
        return TRANSLATIONS.get(key, {}).get(self.lang_code, key)

    def register_widget(self, widget, key, attr="text"):
        """Register a widget for automatic translation updates."""
        self.translatable_widgets.append((widget, key, attr))
        # Set initial value immediately
        if attr == "title":
            widget.title(self.tr(key))
        else:
            try:
                widget.config(**{attr: self.tr(key)})
            except:
                pass # Ignore if config fails (e.g. some widgets might differ)
        return widget

    def update_language(self, event=None):
        """Update all registered widgets to the current language."""
        # Update lang code from combobox if this is triggered by event
        if hasattr(self, 'lang_combo'):
            selection = self.lang_combo.get()
            if "中文" in selection:
                self.lang_code = "zh"
            else:
                self.lang_code = "en"
            
            # Save new language preference
            self.save_settings()
        
        # Update registered widgets
        for widget, key, attr in self.translatable_widgets:
            val = self.tr(key)
            if attr == "title":
                widget.title(val)
            else:
                try:
                    widget.config(**{attr: val})
                except Exception as e:
                    print(f"Failed to update widget {widget}: {e}")

        # Update dynamic status labels if they are "Ready"
        if self.enc_status.cget("text") in ["Ready", "就绪"]:
             self.enc_status.config(text=self.tr("ready"))
        if self.dec_status.cget("text") in ["Ready", "就绪"]:
             self.dec_status.config(text=self.tr("ready"))

    def setup_ui(self):
        self.geometry("1200x800")
        self.minsize(1000, 700)
        self.register_widget(self, "title", "title")
        
        # --- Top Control Bar ---
        top_frame = ttk.LabelFrame(self, padding=10)
        self.register_widget(top_frame, "control_panel", "text")
        top_frame.pack(fill="x", padx=10, pady=5)
        
        # password
        lbl_pwd = ttk.Label(top_frame)
        self.register_widget(lbl_pwd, "password")
        lbl_pwd.pack(side="left", padx=5)

        self.pwd_entry = ttk.Entry(top_frame, width=20, show="*")
        self.pwd_entry.pack(side="left", padx=5)
        
        self.btn_confirm_pwd = ttk.Button(top_frame, command=self.confirm_password)
        self.register_widget(self.btn_confirm_pwd, "confirm_pwd")
        self.btn_confirm_pwd.pack(side="left", padx=5)
        
        # Algorithm
        lbl_algo = ttk.Label(top_frame)
        self.register_widget(lbl_algo, "algorithm")
        lbl_algo.pack(side="left", padx=5)

        self.algo_var = tk.StringVar(value="AES-256")
        self.algo_combo = ttk.Combobox(top_frame, textvariable=self.algo_var, 
                                       values=["AES-128", "AES-192", "AES-256"], state="readonly", width=10)
        self.algo_combo.pack(side="left", padx=5)
        
        # Language Selector
        lbl_lang = ttk.Label(top_frame)
        self.register_widget(lbl_lang, "lang_label")
        lbl_lang.pack(side="left", padx=5)

        self.lang_combo = ttk.Combobox(top_frame, values=["English", "中文 (Chinese)"], state="readonly", width=15)
        # Set initial selection based on loaded settings
        if self.lang_code == "zh":
            self.lang_combo.set("中文 (Chinese)")
        else:
            self.lang_combo.set("English")
            
        self.lang_combo.pack(side="left", padx=5)
        self.lang_combo.bind("<<ComboboxSelected>>", self.update_language)

        #iterations
        lbl_iter = ttk.Label(top_frame)
        self.register_widget(lbl_iter, "iterations")
        lbl_iter.pack(side="left", padx=5)

        self.iter_entry = ttk.Entry(top_frame, width=20, textvariable=self.iterations_var)
        self.iter_entry.pack(side="left", padx=5)

        self.btn_confirm_iterations = ttk.Button(top_frame, command=self.confirm_iterations)
        self.register_widget(self.btn_confirm_iterations, "confirm_iterations")
        self.btn_confirm_iterations.pack(side="left", padx=5)


        # Output Paths
        btn_enc_out = ttk.Button(top_frame, command=self.select_enc_out_dir)
        self.register_widget(btn_enc_out, "set_enc_out")
        btn_enc_out.pack(side="left", padx=10)

        self.lbl_enc_out = ttk.Label(top_frame, textvariable=self.enc_output_path, foreground="gray")
        self.lbl_enc_out.pack(side="left", padx=2)

        btn_dec_out = ttk.Button(top_frame, command=self.select_dec_out_dir)
        self.register_widget(btn_dec_out, "set_dec_out")
        btn_dec_out.pack(side="left", padx=10)

        self.lbl_dec_out = ttk.Label(top_frame, textvariable=self.dec_output_path, foreground="gray")
        self.lbl_dec_out.pack(side="left", padx=2)

        # --- Main Grid ---
        main_frame = ttk.Frame(self, padding=10)
        main_frame.pack(fill="both", expand=True)
        main_frame.columnconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        main_frame.columnconfigure(2, weight=1)
        main_frame.rowconfigure(0, weight=1)

        # 1. Left: Encrypt Text
        self.create_text_section(main_frame, 0, "text_enc_title", "btn_encrypt", self.action_encrypt_text)
        
        # 2. Middle: Decrypt Text
        self.create_text_section(main_frame, 1, "text_dec_title", "btn_decrypt", self.action_decrypt_text)
        
        # 3. Right: File Operations
        self.create_file_section(main_frame, 2)

    def create_text_section(self, parent, col, title_key, btn_text_key, btn_command):
        frame = ttk.LabelFrame(parent, padding=5)
        self.register_widget(frame, title_key, "text")
        frame.grid(row=0, column=col, sticky="nsew", padx=5, pady=5)
        
        # Top: Result/Log Area (Scrollable)
        log_label = ttk.Label(frame)
        self.register_widget(log_label, "history")
        log_label.pack(anchor="w")
        
        log_area = scrolledtext.ScrolledText(frame, height=15, state="disabled", font=("Consolas", 9))
        log_area.pack(fill="both", expand=True, pady=(0, 5))
        
        if "Encrypt" in self.tr(btn_text_key) or "加密" in self.tr(btn_text_key):
             if btn_text_key == "btn_encrypt":
                self.enc_log = log_area
             else:
                self.dec_log = log_area
        else:
             if btn_text_key == "btn_encrypt":
                self.enc_log = log_area
             else:
                self.dec_log = log_area

        # Bottom: Input Area
        input_frame = ttk.Frame(frame)
        input_frame.pack(fill="x", pady=5)
        
        input_label = ttk.Label(input_frame)
        self.register_widget(input_label, "input_text")
        input_label.pack(anchor="w")
        
        input_text = tk.Text(input_frame, height=8, font=("Arial", 10))
        input_text.pack(fill="x", side="left", expand=True)
        
        if btn_text_key == "btn_encrypt":
            self.enc_input = input_text
        else:
            self.dec_input = input_text

        # Action Button
        btn = ttk.Button(input_frame, command=btn_command)
        self.register_widget(btn, btn_text_key)
        btn.pack(side="right", padx=5, fill="y")

    def create_file_section(self, parent, col):
        frame = ttk.LabelFrame(parent, padding=5)
        self.register_widget(frame, "file_ops_title", "text")
        frame.grid(row=0, column=col, sticky="nsew", padx=5, pady=5)
        frame.rowconfigure(0, weight=1)
        frame.rowconfigure(1, weight=1)
        
        # Upper: Encrypt File
        self.create_single_file_op(frame, 0, "file_enc_title", self.selected_enc_file, "btn_enc_file", self.action_encrypt_file, "enc")
        
        # Lower: Decrypt File
        self.create_single_file_op(frame, 1, "file_dec_title", self.selected_dec_file, "btn_dec_file", self.action_decrypt_file, "dec")

    def create_single_file_op(self, parent, row, title_key, path_var, btn_text_key, btn_command, mode):
        sub_frame = ttk.LabelFrame(parent, padding=5)
        self.register_widget(sub_frame, title_key, "text")
        sub_frame.grid(row=row, column=0, sticky="nsew", padx=2, pady=5)
        
        # Path Display / Drop Zone
        lbl_info = ttk.Label(sub_frame)
        self.register_widget(lbl_info, "drag_info")
        lbl_info.pack(anchor="w")
        
        path_entry = ttk.Entry(sub_frame, textvariable=path_var, state="readonly")
        path_entry.pack(fill="x", pady=5)
        
        # DND
        if HAS_DND:
            path_entry.drop_target_register(DND_FILES)
            path_entry.dnd_bind('<<Drop>>', lambda e: path_var.set(e.data.strip('{}')))

        # Buttons
        btn_frame = ttk.Frame(sub_frame)
        btn_frame.pack(fill="x")
        
        btn_sel = ttk.Button(btn_frame, command=lambda: self.select_file(path_var))
        self.register_widget(btn_sel, "select_file")
        btn_sel.pack(side="left", padx=2)
        
        btn_do = ttk.Button(btn_frame, command=btn_command)
        self.register_widget(btn_do, btn_text_key)
        btn_do.pack(side="right", padx=2)
        
        # Progress Bar
        pb = ttk.Progressbar(sub_frame, orient="horizontal", mode="determinate")
        pb.pack(fill="x", pady=5)
        
        # Status Label
        status_lbl = ttk.Label(sub_frame, text="Ready", font=("Arial", 8))
        status_lbl.pack(anchor="w")
        
        if mode == "enc":
            self.enc_pb = pb
            self.enc_status = status_lbl
        else:
            self.dec_pb = pb
            self.dec_status = status_lbl

    # --- Actions ---

    def log(self, area, message):
        area.config(state="normal")
        timestamp = datetime.datetime.now().strftime("%H:%M:%S")
        area.insert("end", f"[{timestamp}] {message}\n")
        area.insert("end", "-"*30 + "\n")
        area.see("end")
        area.config(state="disabled")

    def get_password(self):
        pwd = self.pwd_entry.get()
        if not pwd:
            messagebox.showwarning(self.tr("missing_pwd_title"), self.tr("missing_pwd_msg"))
            return None
        return pwd

    def confirm_password(self):
        pwd = self.pwd_entry.get()
        if pwd:
            messagebox.showinfo(self.tr("pwd_conf_title"), self.tr("pwd_conf_msg"))
        else:
            messagebox.showwarning(self.tr("warning"), self.tr("pwd_empty_msg"))

    def check_iterations(self):
        try:
            iterations_str = self.iter_entry.get()
            if not iterations_str:
                messagebox.showwarning(self.tr("missing_iterations_title"), self.tr("missing_iterations_msg"))
                return False

            iterations = int(iterations_str)

            if iterations <= 0:
                messagebox.showwarning(self.tr("warning"), self.tr("iterations_empty_msg"))
                return False

            if iterations > self.MAX_ITERATIONS:
                messagebox.showerror(self.tr("error"), self.tr("iterations_max_restrict"))
                return False
            return True
        except ValueError:
            messagebox.showerror(self.tr("error"), "Invalid number format!")
            return False

    def get_iterations(self):
        is_valid_iterations = self.check_iterations()
        if not is_valid_iterations:
            return None
        else:
            iterations = int(self.iter_entry.get())
            if not iterations:
                messagebox.showwarning(self.tr("missing_iterations_title"), self.tr("missing_iterations_msg"))
                return None
            self.crypto.ITERATIONS = iterations
            return iterations

    def confirm_iterations(self):
        is_valid_iterations = self.check_iterations()
        if not is_valid_iterations:
            return None
        else:
            iterations = int(self.iter_entry.get())
            if iterations:
                messagebox.showinfo(self.tr("iterations_conf_title"), self.tr("iterations_conf_msg"))
                self.crypto.ITERATIONS = iterations
                self.save_settings()
            else:
                messagebox.showwarning(self.tr("warning"), self.tr("iterations_empty_msg"))

    def select_enc_out_dir(self):
        d = filedialog.askdirectory()
        if d: 
            self.enc_output_path.set(d)
            self.save_settings()

    def select_dec_out_dir(self):
        d = filedialog.askdirectory()
        if d: 
            self.dec_output_path.set(d)
            self.save_settings()

    def select_file(self, var):
        f = filedialog.askopenfilename()
        if f: var.set(f)

    def action_encrypt_text(self):
        pwd = self.get_password()
        iterations = self.get_iterations()
        if not iterations: return
        if not pwd: return
        
        text = self.enc_input.get("1.0", "end-1c")
        if not text: return
        
        try:
            algo = self.algo_var.get()
            result = self.crypto.encrypt_text(text, pwd, algo)
            self.log(self.enc_log, f"Encrypted ({algo}):\n{result}")
        except Exception as e:
            messagebox.showerror(self.tr("error"), str(e))

    def action_decrypt_text(self):
        pwd = self.get_password()
        iterations = self.get_iterations()
        if not iterations: return
        if not pwd: return
        
        text = self.dec_input.get("1.0", "end-1c")
        if not text: return
        
        try:
            algo = self.algo_var.get()
            result = self.crypto.decrypt_text(text.strip(), pwd, algo)
            self.log(self.dec_log, f"Decrypted ({algo}):\n{result}")
        except Exception as e:
            self.log(self.dec_log, f"Error: {str(e)}")
            messagebox.showerror(self.tr("error"), self.tr("dec_fail_msg"))

    def action_encrypt_file(self):
        self._run_file_op(self.selected_enc_file, self.enc_output_path, 
                          self.crypto.encrypt_file, self.enc_pb, self.enc_status, ".enc", self.tr("file_enc_title"))

    def action_decrypt_file(self):
        self._run_file_op(self.selected_dec_file, self.dec_output_path, 
                          self.crypto.decrypt_file, self.dec_pb, self.dec_status, ".dec", self.tr("file_dec_title"))

    def _run_file_op(self, input_var, output_dir_var, op_func, pb, status_lbl, suffix, op_name):
        pwd = self.get_password()
        iterations = self.get_iterations()
        if not iterations: return
        if not pwd: return
        
        in_path = input_var.get()
        if not in_path or not os.path.exists(in_path):
            messagebox.showwarning(self.tr("file_error_title"), self.tr("file_error_msg"))
            return

        out_dir = output_dir_var.get()
        if out_dir:
            if not os.path.exists(out_dir):
                try:
                    os.makedirs(out_dir)
                except OSError:
                    # If cannot create, fallback to input directory
                    out_dir = ""
        
        if not out_dir:
            out_dir = os.path.dirname(in_path)
        
        base_name = os.path.basename(in_path)
        out_path = os.path.join(out_dir, base_name + suffix)
        
        algo = self.algo_var.get()
        
        # Prepare translated strings for the thread
        str_processing = self.tr("processing")
        str_done = self.tr("done")
        str_complete = self.tr("op_complete")
        str_saved = self.tr("saved_to")
        str_error = self.tr("error")
        
        def worker():
            try:
                # Update UI
                self.after(0, lambda: status_lbl.config(text=str_processing, foreground="blue"))
                self.after(0, lambda: pb.config(value=0))
                
                def progress(current, total):
                    perc = (current / total) * 100
                    self.after(0, lambda: pb.config(value=perc))
                
                start_time = datetime.datetime.now()
                op_func(in_path, out_path, pwd, algo, progress)
                end_time = datetime.datetime.now()
                
                self.after(0, lambda: status_lbl.config(text=f"{str_done} ({end_time - start_time})", foreground="green"))
                self.after(0, lambda: messagebox.showinfo(f"{op_name} {str_complete}", f"{str_saved}\n{out_path}"))
                
            except Exception as e:
                self.after(0, lambda: status_lbl.config(text=f"{str_error}: {str(e)}", foreground="red"))
                self.after(0, lambda: messagebox.showerror(str_error, str(e)))

        threading.Thread(target=worker, daemon=True).start()

if __name__ == "__main__":
    app = EncryptApp()
    app.mainloop()
