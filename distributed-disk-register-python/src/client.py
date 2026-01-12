import socket
import sys

def run_client(port=6666):
    try:
        # Lidere (Socket üzerinden) baglan
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(('localhost', port))
        
        print(f"--- Istemci (TCP Socket) Baslatildi (Port: {port}) ---")
        print("Komutlar: SET <id> <mesaj>, GET <id>, EXIT")

        while True:
            cmd = input("> ")
            if cmd.upper() == "EXIT": break
            
            s.sendall(cmd.encode())
            response = s.recv(1024).decode()
            print(f"Sunucu Yaniti: {response}")

    except Exception as e:
        print(f"Hata: {e}")
    finally:
        s.close()

if __name__ == "__main__":
    run_client()
