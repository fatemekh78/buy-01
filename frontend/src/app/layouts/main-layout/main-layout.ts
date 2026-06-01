import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';

// 🚨 FIX: Strictly adhering to your file naming conventions (no .component in path)
import { Navbar } from '../../components/navbar/navbar'; 
import { SidenavComponent } from '../../components/sidenav/sidenav'; 

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule,
    Navbar,
    SidenavComponent
  ],
  templateUrl: './main-layout.html',
  styleUrls: ['./main-layout.scss'] // Updated to SCSS
})
export class MainLayout { }