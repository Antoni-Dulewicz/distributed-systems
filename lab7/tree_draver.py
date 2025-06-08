from __future__ import annotations
from kazoo.client import KazooClient
from kazoo.protocol.states import EventType
from kazoo.exceptions import NoNodeError
from typing import List
import tkinter as tk
from tkinter import messagebox
import queue

class WatchNode:
    def __init__(self, path: str):
        self.path = path
        self.children: List[WatchNode] = []

def count_all_descendants(path="/a"):
    try:
        if not zk.exists(path):
            return 0
        children = zk.get_children(path)
        count = len(children)
        for child in children:
            count += count_all_descendants(f"{path}/{child}")
        return count
    except NoNodeError:
        return 0
    except Exception as e:
        print(f"Error counting descendants {path}: {e}")
        return 0

def print_descendants_count():
    count = count_all_descendants("/a")
    print(f"Node /a has {count} descendants")
    gui_queue.put(f"Counter: {count}")

def build_node_tree(path: str) -> WatchNode:
    node = WatchNode(path)
    try:
        children = zk.get_children(path)
        for child in children:
            child_node = build_node_tree(f"{path}/{child}")
            node.children.append(child_node)
    except NoNodeError:
        pass
    return node

def gui_updater():
    try:
        while not gui_queue.empty():
            msg = gui_queue.get_nowait()
            var.set(msg)
    except queue.Empty:
        pass
    root.after(500, gui_updater)

def draw_tree(canvas, node, x, y, x_spacing=80, y_spacing=60, parent_coords=None):
    canvas.create_oval(x-15, y-15, x+15, y+15, fill="lightblue")
    canvas.create_text(x, y, text=node.path.split("/")[-1] or "/", font=("Helvetica", 10))

    if parent_coords:
        canvas.create_line(parent_coords[0], parent_coords[1], x, y)

    n = len(node.children)
    if n == 0:
        return

    total_width = (n - 1) * x_spacing
    start_x = x - total_width // 2

    for i, child in enumerate(node.children):
        draw_tree(canvas, child, start_x + i * x_spacing, y + y_spacing, x_spacing, y_spacing, (x, y))

def show_tree():
    if not zk.exists("/a"):
        messagebox.showinfo("Struktura drzewa", "Brak danych (węzeł /a nie istnieje)")
        return

    root_node = build_node_tree("/a")
    tree_window = tk.Toplevel()
    tree_window.title("Drzewo")
    canvas = tk.Canvas(tree_window, width=800, height=600, bg="white")
    canvas.pack(expand=True, fill="both")
    draw_tree(canvas, root_node, x=400, y=40)

def watch_node_recursively(node: WatchNode):
    if node.path in watched_paths:
        return
    if not zk.exists(node.path):
        print(f"Node {node.path} does not exist...")
        return

    watched_paths.add(node.path)

    @zk.DataWatch(node.path)
    def data_watch(data, stat, event):
        if event is not None:
            print(f"Node {node.path} {str(event.type).lower()}")
            if event.type == EventType.DELETED:
                watched_paths.discard(node.path)
                print_descendants_count()
                return False

    @zk.ChildrenWatch(node.path)
    def children_watch(children):
        node.children.clear()
        for child_name in children:
            child_path = f"{node.path}/{child_name}"
            child_node = WatchNode(child_path)
            node.children.append(child_node)
            watch_node_recursively(child_node)
        if children is not None:
            print_descendants_count()

def watch_root():
    @zk.DataWatch("/a")
    def root_watch(data, stat, event):
        if event is not None:
            if event.type == EventType.CREATED:
                print("Node /a created")
                root_node = WatchNode("/a")
                watch_node_recursively(root_node)
                print_descendants_count()
                root.deiconify()
            elif event.type == EventType.DELETED:
                print("Node /a deleted")
                watched_paths.discard("/a")
                root.withdraw()

def on_close():
    print("Closing...")
    zk.stop()
    zk.close()
    root.destroy()

zk = KazooClient(hosts='localhost:2181')
zk.start()

watched_paths = set()
gui_queue = queue.Queue()

root = tk.Tk()
root.withdraw()
root.title("Licznik")

var = tk.StringVar()
var.set("Licznik: 0")

label = tk.Label(root, textvariable=var, font=("Helvetica", 20))
label.pack(padx=40, pady=20)

btn = tk.Button(root, text="Pokaż drzewo", command=show_tree)
btn.pack(pady=10)

root.after(500, gui_updater)
root.protocol("WM_DELETE_WINDOW", on_close)

watch_root()
root.mainloop()