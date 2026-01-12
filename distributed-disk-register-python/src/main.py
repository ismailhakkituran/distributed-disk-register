import sys
import os
import argparse

# Src klasörünü path'e ekle
sys.path.append(os.path.abspath(os.path.dirname(__file__)))
import server
import node

def main():
    parser = argparse.ArgumentParser(description="Distributed Disk Register - HaToKuSe Sistemi")
    parser.add_argument("--mode", type=str, required=True, choices=["leader", "node"],
                        help="Mod seçimi: leader (Beyin) veya node (İşçi)")
    parser.add_argument("--port", type=str, help="Port numarası")
    parser.add_argument("--id", type=int, help="Node ID'si (sadece node modu için)")
    parser.add_argument("--io-mode", type=str, default="buffered", choices=["buffered", "unbuffered"],
                        help="IO modu (sadece node için)")
    
    args = parser.parse_args()

    print("=" * 60)
    print("     DAĞITIK MESAJ KAYIT SİSTEMİ (HaToKuSe)")
    print("=" * 60)
    
    try:
        if args.mode == "leader":
            print("\n[MOD] Lider (Koordinatör/Beyin) başlatılıyor...")
            grpc_port = args.port or "5550"
            server.serve(grpc_port=grpc_port, socket_port=6666)
        
        elif args.mode == "node":
            if not args.id or not args.port:
                print("\n[HATA] Node modu için --id ve --port parametreleri zorunludur!")
                print("Örnek: python main.py --mode node --id 1 --port 50061")
                sys.exit(1)
            print(f"\n[MOD] Node (İşçi) başlatılıyor... ID={args.id}, Port={args.port}")
            node.serve(args.id, args.port, io_mode=args.io_mode)
    
    except KeyboardInterrupt:
        print("\n\n[BİLGİ] Sistem kapatılıyor...")
        sys.exit(0)

if __name__ == "__main__":
    main()
